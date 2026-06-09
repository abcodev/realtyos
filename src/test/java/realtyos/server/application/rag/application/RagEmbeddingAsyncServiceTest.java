package realtyos.server.application.rag.application;

import org.junit.jupiter.api.Test;
import realtyos.server.application.common.outbox.OutboxEventService;
import realtyos.server.application.rag.domain.RagAsyncJob;
import realtyos.server.application.rag.domain.RagEmbeddingRequestedEvent;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RagEmbeddingAsyncServiceTest {

    @Test
    void createsSagaAndOutboxEventForAsyncEmbeddingRequest() {
        FakeSagaService sagaService = new FakeSagaService();
        FakeOutboxEventService outboxEventService = new FakeOutboxEventService();
        RagEmbeddingAsyncService service = new RagEmbeddingAsyncService(sagaService, outboxEventService);

        RagAsyncJob job = service.requestEmbeddingBuild(100, "OLLAMA", "nomic-embed-text");

        assertThat(job.sagaId()).isEqualTo(sagaService.sagaId);
        assertThat(job.outboxEventId()).isEqualTo(outboxEventService.eventId);
        assertThat(job.status()).isEqualTo("REQUESTED");
        assertThat(outboxEventService.aggregateType).isEqualTo("RAG_EMBEDDING_SAGA");
        assertThat(outboxEventService.eventType).isEqualTo("RAG_EMBEDDING_REQUESTED");
        assertThat(outboxEventService.payload).isInstanceOf(RagEmbeddingRequestedEvent.class);

        RagEmbeddingRequestedEvent event = (RagEmbeddingRequestedEvent) outboxEventService.payload;
        assertThat(event.sagaId()).isEqualTo(sagaService.sagaId);
        assertThat(event.limit()).isEqualTo(100);
        assertThat(event.provider()).isEqualTo("OLLAMA");
        assertThat(event.model()).isEqualTo("nomic-embed-text");
        assertThat(event.attempt()).isEqualTo(1);
    }

    private static class FakeSagaService extends RagEmbeddingSagaService {

        private final UUID sagaId = UUID.randomUUID();

        private FakeSagaService() {
            super(null);
        }

        @Override
        public UUID start(int limit, String provider, String model) {
            return sagaId;
        }
    }

    private static class FakeOutboxEventService extends OutboxEventService {

        private final UUID eventId = UUID.randomUUID();
        private String aggregateType;
        private String eventType;
        private Object payload;

        private FakeOutboxEventService() {
            super(null, null);
        }

        @Override
        public UUID append(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
            this.aggregateType = aggregateType;
            this.eventType = eventType;
            this.payload = payload;
            return eventId;
        }
    }
}
