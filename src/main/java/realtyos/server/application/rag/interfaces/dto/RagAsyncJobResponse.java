package realtyos.server.application.rag.interfaces.dto;

import realtyos.server.application.rag.domain.RagAsyncJob;

import java.util.UUID;

public record RagAsyncJobResponse(
        UUID sagaId,
        UUID outboxEventId,
        String status
) {
    public static RagAsyncJobResponse from(RagAsyncJob job) {
        return new RagAsyncJobResponse(job.sagaId(), job.outboxEventId(), job.status());
    }
}
