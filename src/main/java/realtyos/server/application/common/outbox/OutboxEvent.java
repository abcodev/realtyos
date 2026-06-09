package realtyos.server.application.common.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String topic,
        String payload,
        String status,
        int retryCount,
        LocalDateTime nextAttemptAt,
        String lastError
) {
}
