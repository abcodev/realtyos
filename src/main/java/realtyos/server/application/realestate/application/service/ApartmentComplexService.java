package realtyos.server.application.realestate.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.realestate.domain.ApartmentComplex;
import realtyos.server.application.realestate.domain.ApartmentComplexBasisInfo;
import realtyos.server.application.realestate.domain.ApartmentComplexBasisInfoRepository;
import realtyos.server.application.realestate.domain.ApartmentComplexFetchResult;
import realtyos.server.application.realestate.domain.ApartmentComplexRepository;
import realtyos.server.application.realestate.domain.DataFetchPort;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApartmentComplexService {

    private static final int DEFAULT_BASIS_INFO_BATCH_SIZE = 500;

    private final DataFetchPort fetchPort;
    private final ApartmentComplexRepository repository;
    private final ApartmentComplexBasisInfoRepository basisInfoRepository;

    public ApartmentComplexSyncResult fetchAndSaveTotalAptList(int pageNo, int numOfRows) {
        AtomicInteger upsertedCount = new AtomicInteger();
        ApartmentComplexFetchResult fetchResult = fetchPort.fetchApartmentComplexes(pageNo, numOfRows, complexes -> {
            int upserted = upsertComplexes(complexes);
            upsertedCount.addAndGet(upserted);
        });

        log.info("아파트 단지 정보 수집 완료 - pageNo: {}, numOfRows: {}, fetched: {}, upserted: {}, totalCount: {}",
                pageNo, numOfRows, fetchResult.fetchedCount(), upsertedCount.get(), fetchResult.totalCount());

        return ApartmentComplexSyncResult.builder()
                .requestedPageNo(pageNo)
                .requestedNumOfRows(numOfRows)
                .totalCount(fetchResult.totalCount())
                .fetchedCount(fetchResult.fetchedCount())
                .upsertedCount(upsertedCount.get())
                .deactivatedCount(0)
                .build();
    }

    @Transactional
    protected int upsertComplexes(List<ApartmentComplex> complexes) {
        List<ApartmentComplex> validComplexes = complexes.stream()
                .filter(complex -> complex.kaptCode() != null && !complex.kaptCode().isBlank())
                .toList();

        if (validComplexes.isEmpty()) {
            return 0;
        }

        return repository.upsertAll(validComplexes);
    }

    public ApartmentComplexSyncResult refreshAllComplexes(int numOfRows) {
        ApartmentComplexSyncResult syncResult = fetchAndSaveTotalAptList(1, numOfRows);
        int deactivatedComplexes = repository.markInactiveIfNotSynced();
        int deactivatedBasisInfos = basisInfoRepository.markInactiveForInactiveComplexes();

        log.info("아파트 단지 월간 새로고침 완료 - upserted: {}, deactivatedComplexes: {}, deactivatedBasisInfos: {}",
                syncResult.upsertedCount(), deactivatedComplexes, deactivatedBasisInfos);

        return ApartmentComplexSyncResult.builder()
                .requestedPageNo(syncResult.requestedPageNo())
                .requestedNumOfRows(syncResult.requestedNumOfRows())
                .totalCount(syncResult.totalCount())
                .fetchedCount(syncResult.fetchedCount())
                .upsertedCount(syncResult.upsertedCount())
                .deactivatedCount(deactivatedComplexes)
                .build();
    }

    public ApartmentComplexBasisInfoSyncResult fetchAndSaveBasisInfo(int limit) {
        int normalizedLimit = Math.max(limit, 1);
        List<String> kaptCodes = repository.findActiveKaptCodes(normalizedLimit);
        int savedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String kaptCode : kaptCodes) {
            if (kaptCode == null || kaptCode.isBlank()) {
                skippedCount++;
                continue;
            }

            try {
                ApartmentComplexBasisInfo basisInfo = fetchPort.fetchApartmentComplexBasisInfo(kaptCode)
                        .orElse(null);
                if (basisInfo == null || basisInfo.kaptCode() == null || basisInfo.kaptCode().isBlank()) {
                    skippedCount++;
                    continue;
                }

                basisInfoRepository.upsert(basisInfo);
                savedCount++;
            } catch (Exception e) {
                failedCount++;
                log.warn("아파트 기본정보 저장 실패 - kaptCode: {}", kaptCode, e);
            }
        }

        log.info("아파트 기본정보 수집 완료 - requestedLimit: {}, target: {}, saved: {}, skipped: {}, failed: {}",
                normalizedLimit, kaptCodes.size(), savedCount, skippedCount, failedCount);

        return ApartmentComplexBasisInfoSyncResult.builder()
                .requestedLimit(normalizedLimit)
                .targetCount(kaptCodes.size())
                .savedCount(savedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .build();
    }

    public ApartmentComplexBasisInfoSyncResult fetchAndSaveMissingBasisInfo(int limit) {
        int normalizedLimit = Math.max(limit, 1);
        List<String> kaptCodes = repository.findKaptCodesWithoutBasisInfo(normalizedLimit);
        return fetchAndSaveBasisInfoByKaptCodes(normalizedLimit, kaptCodes);
    }

    public ApartmentComplexBasisInfoSyncResult fetchAndSaveAllMissingBasisInfo() {
        int totalTargetCount = 0;
        int totalSavedCount = 0;
        int totalSkippedCount = 0;
        int totalFailedCount = 0;

        while (true) {
            List<String> kaptCodes = repository.findKaptCodesWithoutBasisInfo(DEFAULT_BASIS_INFO_BATCH_SIZE);
            if (kaptCodes.isEmpty()) {
                break;
            }

            ApartmentComplexBasisInfoSyncResult batchResult = fetchAndSaveBasisInfoByKaptCodes(
                    DEFAULT_BASIS_INFO_BATCH_SIZE,
                    kaptCodes
            );

            totalTargetCount += batchResult.targetCount();
            totalSavedCount += batchResult.savedCount();
            totalSkippedCount += batchResult.skippedCount();
            totalFailedCount += batchResult.failedCount();

            log.info("아파트 누락 기본정보 전체 수집 진행 - batchTarget: {}, totalSaved: {}, totalSkipped: {}, totalFailed: {}",
                    batchResult.targetCount(), totalSavedCount, totalSkippedCount, totalFailedCount);

            if (batchResult.savedCount() == 0) {
                log.warn("아파트 누락 기본정보 수집 중단 - 현재 배치에서 저장된 데이터가 없습니다. skipped: {}, failed: {}",
                        batchResult.skippedCount(), batchResult.failedCount());
                break;
            }
        }

        return ApartmentComplexBasisInfoSyncResult.builder()
                .requestedLimit(0)
                .targetCount(totalTargetCount)
                .savedCount(totalSavedCount)
                .skippedCount(totalSkippedCount)
                .failedCount(totalFailedCount)
                .build();
    }

    private ApartmentComplexBasisInfoSyncResult fetchAndSaveBasisInfoByKaptCodes(int requestedLimit, List<String> kaptCodes) {
        int savedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String kaptCode : kaptCodes) {
            try {
                ApartmentComplexBasisInfo basisInfo = fetchPort.fetchApartmentComplexBasisInfo(kaptCode)
                        .orElse(null);
                if (basisInfo == null || basisInfo.kaptCode() == null || basisInfo.kaptCode().isBlank()) {
                    skippedCount++;
                    continue;
                }

                basisInfoRepository.upsert(basisInfo);
                savedCount++;
            } catch (Exception e) {
                failedCount++;
                log.warn("아파트 기본정보 저장 실패 - kaptCode: {}", kaptCode, e);
            }
        }

        return ApartmentComplexBasisInfoSyncResult.builder()
                .requestedLimit(requestedLimit)
                .targetCount(kaptCodes.size())
                .savedCount(savedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .build();
    }
}
