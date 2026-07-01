package realtyos.server.application.realestate.interfaces.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import realtyos.server.application.realestate.application.service.ApartmentComplexService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApartmentComplexBatchScheduler {

    private final ApartmentComplexService service;

    @Scheduled(cron = "${app.realestate.apartment-complex.monthly-sync-cron:0 10 2 1 * ?}", zone = "Asia/Seoul")
    public void scheduleMonthlyApartmentComplexRefresh() {
        log.info("아파트 단지 월간 동기화 시작");

        try {
            service.refreshAllComplexes(1000);
            service.fetchAndSaveAllMissingBasisInfo();
            log.info("아파트 단지 월간 동기화 완료");
        } catch (Exception e) {
            log.error("아파트 단지 월간 동기화 실패", e);
        }
    }
}
