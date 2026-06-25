package realtyos.server.application.rag.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.common.outbox.OutboxEventService;
import realtyos.server.application.rag.domain.RagAsyncJob;
import realtyos.server.application.rag.domain.RagEmbeddingRequestedEvent;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagEmbeddingAsyncService {

    private final RagEmbeddingSagaService sagaService;
    private final OutboxEventService outboxEventService;

    @Value("${app.kafka.topics.rag-embedding-requested:realtyos.rag.embedding.requested}")
    private String embeddingRequestedTopic;

    @Transactional
    public RagAsyncJob requestEmbeddingBuild(int limit, String provider, String model) {
        UUID sagaId = sagaService.start(limit, provider, model);
        RagEmbeddingRequestedEvent event = new RagEmbeddingRequestedEvent(sagaId, limit, provider, model, 1);
        UUID outboxEventId = outboxEventService.append(
                "RAG_EMBEDDING_SAGA",
                sagaId.toString(),
                "RAG_EMBEDDING_REQUESTED",
                embeddingRequestedTopic,
                event
        );
        return new RagAsyncJob(sagaId, outboxEventId, "REQUESTED");
    }
}
