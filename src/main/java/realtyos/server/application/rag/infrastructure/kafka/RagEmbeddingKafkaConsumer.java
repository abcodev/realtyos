package realtyos.server.application.rag.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import realtyos.server.application.rag.application.RagEmbeddingBuildService;
import realtyos.server.application.rag.application.RagEmbeddingSagaService;
import realtyos.server.application.rag.domain.RagEmbeddingBuildResult;
import realtyos.server.application.rag.domain.RagEmbeddingRequestedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class RagEmbeddingKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RagEmbeddingBuildService embeddingBuildService;
    private final RagEmbeddingSagaService sagaService;

    @Value("${app.kafka.topics.rag-embedding-retry:realtyos.rag.embedding.retry}")
    private String retryTopic;

    @Value("${app.kafka.topics.rag-embedding-dlq:realtyos.rag.embedding.dlq}")
    private String dlqTopic;

    @Value("${app.kafka.retry.max-attempts:3}")
    private int maxAttempts;

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.rag-embedding-requested:realtyos.rag.embedding.requested}",
                    "${app.kafka.topics.rag-embedding-retry:realtyos.rag.embedding.retry}"
            },
            groupId = "${app.kafka.consumer-group:realtyos-rag-workers}"
    )
    public void consumeEmbeddingRequest(String message) {
        RagEmbeddingRequestedEvent event = readEvent(message);
        try {
            sagaService.markProcessing(event.sagaId(), event.attempt());
            RagEmbeddingBuildResult result = embeddingBuildService.buildDocumentEmbeddings(
                    event.limit(),
                    event.provider(),
                    event.model()
            );
            sagaService.markCompleted(event.sagaId(), result);
            log.info("RAG embedding saga completed - sagaId: {}, provider: {}, model: {}, embedded: {}",
                    event.sagaId(), result.provider(), result.model(), result.embeddedCount());
        } catch (Exception e) {
            handleFailure(event, e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.rag-embedding-dlq:realtyos.rag.embedding.dlq}",
            groupId = "${app.kafka.consumer-group:realtyos-rag-workers}.dlq"
    )
    public void consumeDlq(String message) {
        RagEmbeddingRequestedEvent event = readEvent(message);
        sagaService.markFailed(event.sagaId(), "DLQ consumed after max retry attempts");
        log.error("RAG embedding event moved to DLQ - sagaId: {}, attempt: {}",
                event.sagaId(), event.attempt());
    }

    private void handleFailure(RagEmbeddingRequestedEvent event, Exception e) {
        try {
            if (event.attempt() < maxAttempts) {
                sagaService.markRetrying(event.sagaId(), e.getMessage());
                RagEmbeddingRequestedEvent retryEvent = event.nextAttempt();
                kafkaTemplate.send(retryTopic, event.sagaId().toString(), objectMapper.writeValueAsString(retryEvent));
                log.warn("RAG embedding retry scheduled - sagaId: {}, nextAttempt: {}/{}",
                        event.sagaId(), retryEvent.attempt(), maxAttempts, e);
                return;
            }

            kafkaTemplate.send(dlqTopic, event.sagaId().toString(), objectMapper.writeValueAsString(event));
            log.error("RAG embedding event sent to DLQ - sagaId: {}, attempt: {}",
                    event.sagaId(), event.attempt(), e);
        } catch (Exception publishError) {
            sagaService.markFailed(event.sagaId(), publishError.getMessage());
            throw new IllegalStateException("retry/DLQ 이벤트 발행에 실패했습니다.", publishError);
        }
    }

    private RagEmbeddingRequestedEvent readEvent(String message) {
        try {
            return objectMapper.readValue(message, RagEmbeddingRequestedEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("RAG embedding 이벤트 역직렬화에 실패했습니다.", e);
        }
    }
}
