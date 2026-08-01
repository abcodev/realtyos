package realtyos.server.application.realestate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import realtyos.server.application.realestate.application.service.ApartmentComplexBasisInfoSyncResult;
import realtyos.server.application.realestate.application.service.ApartmentComplexService;
import realtyos.server.application.realestate.application.service.ApartmentComplexSyncResult;
import realtyos.server.application.realestate.infrastructure.jpa.repository.ApartmentComplexBasisInfoJpaRepository;
import realtyos.server.application.realestate.infrastructure.jpa.repository.ApartmentComplexJpaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = {
        "external.api.realestate.service-key="
})
@ActiveProfiles("loc")
class ApartmentComplexIntegrationTest {

    @Autowired
    private ApartmentComplexService apartmentComplexService;

    @Autowired
    private ApartmentComplexJpaRepository apartmentComplexRepository;

    @Autowired
    private ApartmentComplexBasisInfoJpaRepository apartmentComplexBasisInfoRepository;

    @Test
    @Timeout(value = 6, unit = TimeUnit.HOURS)
    @DisplayName("공공데이터 아파트 목록과 전체 기본정보를 실제 조회해서 DB에 저장한다")
    void fetchAndSaveAllApartmentComplexAndBasisInfo() {
        long initialComplexCount = apartmentComplexRepository.count();
        long initialBasisInfoCount = apartmentComplexBasisInfoRepository.count();

        ApartmentComplexSyncResult complexResult = apartmentComplexService.fetchAndSaveTotalAptList(1, 1000);
        ApartmentComplexBasisInfoSyncResult basisInfoResult = apartmentComplexService.fetchAndSaveAllMissingBasisInfo();

        long finalComplexCount = apartmentComplexRepository.count();
        long finalBasisInfoCount = apartmentComplexBasisInfoRepository.count();

        assertThat(complexResult.fetchedCount()).isGreaterThan(0);
        assertThat(complexResult.upsertedCount()).isGreaterThan(0);
        assertThat(basisInfoResult.failedCount()).isZero();
        assertThat(finalComplexCount).isGreaterThanOrEqualTo(initialComplexCount);
        assertThat(finalBasisInfoCount).isGreaterThanOrEqualTo(initialBasisInfoCount);
        assertThat(finalComplexCount).isGreaterThan(0);
        assertThat(finalBasisInfoCount).isEqualTo(finalComplexCount);
    }
}
