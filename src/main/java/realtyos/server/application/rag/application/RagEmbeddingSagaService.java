package realtyos.server.application.rag.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.rag.domain.RagEmbeddingBuildResult;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RagEmbeddingSagaService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public UUID start(int limit, String provider, String model) {
        UUID sagaId = UUID.randomUUID();
        jdbcTemplate.update("""
                        INSERT INTO rag_embedding_saga (
                            id, status, provider, model, embedding_limit, attempt_count,
                            created_at, updated_at
                        )
                        VALUES (?, 'REQUESTED', ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                sagaId,
                provider,
                model,
                limit
        );
        return sagaId;
    }

    @Transactional
    public void markProcessing(UUID sagaId, int attempt) {
        jdbcTemplate.update("""
                        UPDATE rag_embedding_saga
                        SET status = 'PROCESSING',
                            attempt_count = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                attempt,
                sagaId
        );
    }

    @Transactional
    public void markCompleted(UUID sagaId, RagEmbeddingBuildResult result) {
        jdbcTemplate.update("""
                        UPDATE rag_embedding_saga
                        SET status = 'COMPLETED',
                            provider = ?,
                            model = ?,
                            embedded_count = ?,
                            skipped_count = ?,
                            failed_count = ?,
                            completed_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP,
                            last_error = NULL
                        WHERE id = ?
                        """,
                result.provider(),
                result.model(),
                result.embeddedCount(),
                result.skippedCount(),
                result.failedCount(),
                sagaId
        );
    }

    @Transactional
    public void markRetrying(UUID sagaId, String errorMessage) {
        jdbcTemplate.update("""
                        UPDATE rag_embedding_saga
                        SET status = 'RETRYING',
                            last_error = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                truncate(errorMessage),
                sagaId
        );
    }

    @Transactional
    public void markFailed(UUID sagaId, String errorMessage) {
        jdbcTemplate.update("""
                        UPDATE rag_embedding_saga
                        SET status = 'FAILED',
                            failed_count = failed_count + 1,
                            last_error = ?,
                            completed_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                truncate(errorMessage),
                sagaId
        );
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 2000 ? text : text.substring(0, 2000);
    }
}
