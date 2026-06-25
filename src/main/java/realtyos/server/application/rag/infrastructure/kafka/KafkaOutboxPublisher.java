package realtyos.server.application.rag.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import realtyos.server.application.common.outbox.OutboxEvent;
import realtyos.server.application.common.outbox.OutboxRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.outbox", name = "enabled", havingValue = "true")
public class KafkaOutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.outbox.publish-limit:50}")
    private int publishLimit;

    @Value("${app.kafka.outbox.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findPublishable(publishLimit);
        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.topic(), event.aggregateId(), event.payload()).get(10, TimeUnit.SECONDS);
            outboxRepository.markPublished(event.id());
            log.info("Outbox event published - id: {}, topic: {}, type: {}",
                    event.id(), event.topic(), event.eventType());
        } catch (Exception e) {
            int nextRetryCount = event.retryCount() + 1;
            outboxRepository.markFailed(event.id(), nextRetryCount, maxRetries, e.getMessage());
            log.error("Outbox event publish failed - id: {}, topic: {}, retry: {}/{}",
                    event.id(), event.topic(), nextRetryCount, maxRetries, e);
        }
    }
}
