package realtyos.server.application.rag.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.common.exception.BusinessException;
import realtyos.server.application.common.exception.ErrorCode;
import realtyos.server.application.rag.domain.RagEmbeddingBuildResult;
import realtyos.server.application.rag.domain.RagEmbeddingJobStatus;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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

    @Transactional(readOnly = true)
    public RagEmbeddingJobStatus getJobStatus(UUID sagaId) {
        List<RagEmbeddingJobStatus> results = jdbcTemplate.query("""
                        SELECT id, status, provider, model, embedding_limit, attempt_count,
                               embedded_count, skipped_count, failed_count, last_error,
                               created_at, updated_at, completed_at
                        FROM rag_embedding_saga
                        WHERE id = ?
                        """,
                (rs, rowNum) -> new RagEmbeddingJobStatus(
                        rs.getObject("id", UUID.class),
                        rs.getString("status"),
                        rs.getString("provider"),
                        rs.getString("model"),
                        rs.getInt("embedding_limit"),
                        rs.getInt("attempt_count"),
                        rs.getInt("embedded_count"),
                        rs.getInt("skipped_count"),
                        rs.getInt("failed_count"),
                        rs.getString("last_error"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("completed_at"))
                ),
                sagaId
        );
        return results.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "임베딩 작업을 찾을 수 없습니다: " + sagaId));
    }

    @Transactional(readOnly = true)
    public List<RagEmbeddingJobStatus> findJobStatuses(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, status, provider, model, embedding_limit, attempt_count,
                               embedded_count, skipped_count, failed_count, last_error,
                               created_at, updated_at, completed_at
                        FROM rag_embedding_saga
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new RagEmbeddingJobStatus(
                        rs.getObject("id", UUID.class),
                        rs.getString("status"),
                        rs.getString("provider"),
                        rs.getString("model"),
                        rs.getInt("embedding_limit"),
                        rs.getInt("attempt_count"),
                        rs.getInt("embedded_count"),
                        rs.getInt("skipped_count"),
                        rs.getInt("failed_count"),
                        rs.getString("last_error"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at")),
                        toLocalDateTime(rs.getTimestamp("completed_at"))
                ),
                Math.max(1, Math.min(100, limit))
        );
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 2000 ? text : text.substring(0, 2000);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
