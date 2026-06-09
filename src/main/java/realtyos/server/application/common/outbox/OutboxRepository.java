package realtyos.server.application.common.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public void save(UUID id, String aggregateType, String aggregateId, String eventType, String topic, String payload) {
        jdbcTemplate.update("""
                        INSERT INTO event_outbox (
                            id, aggregate_type, aggregate_id, event_type, topic, payload, status,
                            retry_count, next_attempt_at, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                id,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                payload
        );
    }

    public List<OutboxEvent> findPublishable(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, aggregate_type, aggregate_id, event_type, topic, payload, status,
                               retry_count, next_attempt_at, last_error
                        FROM event_outbox
                        WHERE status IN ('PENDING', 'FAILED')
                        AND next_attempt_at <= CURRENT_TIMESTAMP
                        ORDER BY created_at
                        LIMIT ?
                        """,
                (rs, rowNum) -> new OutboxEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getString("topic"),
                        rs.getString("payload"),
                        rs.getString("status"),
                        rs.getInt("retry_count"),
                        toLocalDateTime(rs.getTimestamp("next_attempt_at")),
                        rs.getString("last_error")
                ),
                Math.max(1, limit)
        );
    }

    public void markPublished(UUID id) {
        jdbcTemplate.update("""
                        UPDATE event_outbox
                        SET status = 'PUBLISHED',
                            published_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP,
                            last_error = NULL
                        WHERE id = ?
                        """,
                id
        );
    }

    public void markFailed(UUID id, int nextRetryCount, int maxRetries, String errorMessage) {
        String status = nextRetryCount >= maxRetries ? "DEAD" : "FAILED";
        jdbcTemplate.update("""
                        UPDATE event_outbox
                        SET status = ?,
                            retry_count = ?,
                            next_attempt_at = ?,
                            updated_at = CURRENT_TIMESTAMP,
                            last_error = ?
                        WHERE id = ?
                        """,
                status,
                nextRetryCount,
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(backoffSeconds(nextRetryCount))),
                truncate(errorMessage),
                id
        );
    }

    private long backoffSeconds(int retryCount) {
        return Math.min(300, (long) Math.pow(2, Math.max(1, retryCount)));
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
