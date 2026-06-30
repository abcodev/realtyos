package realtyos.server.application.realestate.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.realestate.domain.ApartmentComplex;
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
}
