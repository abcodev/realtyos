package realtyos.server.application.rag.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record RagEmbeddingJobStatus(
        UUID sagaId,
        String status,
        String provider,
        String model,
        int embeddingLimit,
        int attemptCount,
        int embeddedCount,
        int skippedCount,
        int failedCount,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
