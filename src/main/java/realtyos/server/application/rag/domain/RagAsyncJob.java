package realtyos.server.application.rag.domain;

import java.util.UUID;

public record RagAsyncJob(
        UUID sagaId,
        UUID outboxEventId,
        String status
) {
}
