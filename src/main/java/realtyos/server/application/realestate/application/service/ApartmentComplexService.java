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

    private final DataFetchPort fetchPort;
    private final ApartmentComplexRepository repository;
    private final ApartmentComplexBasisInfoRepository basisInfoRepository;

    public ApartmentComplexSyncResult fetchAndSaveTotalAptList(int pageNo, int numOfRows) {
        AtomicInteger savedCount = new AtomicInteger();
        ApartmentComplexFetchResult fetchResult = fetchPort.fetchApartmentComplexes(pageNo, numOfRows, complexes -> {
            int saved = saveNewComplexes(complexes);
            savedCount.addAndGet(saved);
        });

        log.info("아파트 단지 정보 수집 완료 - pageNo: {}, numOfRows: {}, fetched: {}, saved: {}, totalCount: {}",
                pageNo, numOfRows, fetchResult.fetchedCount(), savedCount.get(), fetchResult.totalCount());

        return ApartmentComplexSyncResult.builder()
                .requestedPageNo(pageNo)
                .requestedNumOfRows(numOfRows)
                .totalCount(fetchResult.totalCount())
                .fetchedCount(fetchResult.fetchedCount())
                .savedCount(savedCount.get())
                .build();
    }

    @Transactional
    protected int saveNewComplexes(List<ApartmentComplex> complexes) {
        List<ApartmentComplex> newComplexes = complexes.stream()
                .filter(complex -> complex.kaptCode() != null && !complex.kaptCode().isBlank())
                .filter(complex -> !repository.existsByKaptCode(complex.kaptCode()))
                .toList();

        if (newComplexes.isEmpty()) {
            return 0;
        }

        repository.saveAll(newComplexes);
        return newComplexes.size();
    }

    public ApartmentComplexBasisInfoSyncResult fetchAndSaveBasisInfo(int limit) {
        int normalizedLimit = Math.max(limit, 1);
        List<String> kaptCodes = repository.findKaptCodesWithoutBasisInfo(normalizedLimit);
        int savedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (String kaptCode : kaptCodes) {
            if (kaptCode == null || kaptCode.isBlank() || basisInfoRepository.existsByKaptCode(kaptCode)) {
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

                basisInfoRepository.save(basisInfo);
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
}
