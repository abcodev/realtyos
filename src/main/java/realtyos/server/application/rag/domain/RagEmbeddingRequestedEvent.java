package realtyos.server.application.rag.domain;

import java.util.UUID;

public record RagEmbeddingRequestedEvent(
        UUID sagaId,
        int limit,
        String provider,
        String model,
        int attempt
) {
    public RagEmbeddingRequestedEvent nextAttempt() {
        return new RagEmbeddingRequestedEvent(sagaId, limit, provider, model, attempt + 1);
    }
}
