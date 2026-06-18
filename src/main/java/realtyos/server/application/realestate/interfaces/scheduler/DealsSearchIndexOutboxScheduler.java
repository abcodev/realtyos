package realtyos.server.application.realestate.interfaces.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import realtyos.server.application.realestate.application.service.DealsSearchIndexOutboxService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DealsSearchIndexOutboxScheduler {

    private final DealsSearchIndexOutboxService outboxService;

    @Value("${app.elasticsearch.outbox.enabled:true}")
    private boolean enabled;

    @Value("${app.elasticsearch.outbox.poll-limit:1000}")
    private int pollLimit;

    @Value("${app.elasticsearch.outbox.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${app.elasticsearch.outbox.fixed-delay-ms:5000}")
    public void processPendingIndexRequests() {
        if (!enabled) {
            return;
        }
        outboxService.processPending(pollLimit, maxRetries);
    }
}
