package realtyos.server.application.rag.interfaces.dto;

import realtyos.server.application.rag.domain.RagEmbeddingJobStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RagEmbeddingJobStatusResponse(
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
    public static RagEmbeddingJobStatusResponse from(RagEmbeddingJobStatus status) {
        return new RagEmbeddingJobStatusResponse(
                status.sagaId(),
                status.status(),
                status.provider(),
                status.model(),
                status.embeddingLimit(),
                status.attemptCount(),
                status.embeddedCount(),
                status.skippedCount(),
                status.failedCount(),
                status.lastError(),
                status.createdAt(),
                status.updatedAt(),
                status.completedAt()
        );
    }
}
