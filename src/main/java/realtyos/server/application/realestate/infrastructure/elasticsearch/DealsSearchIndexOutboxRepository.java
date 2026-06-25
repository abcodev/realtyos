package realtyos.server.application.realestate.infrastructure.elasticsearch;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import realtyos.server.application.realestate.domain.DealsSearchIndexQueue;
import realtyos.server.application.realestate.domain.DealsSearchIndexRequest;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DealsSearchIndexOutboxRepository implements DealsSearchIndexQueue {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void enqueueAll(List<Long> dealIds) {
        if (dealIds == null || dealIds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                        INSERT INTO real_estate_deals_search_outbox (
                            deal_id, status, retry_count, next_attempt_at, created_at, updated_at
                        )
                        VALUES (?, 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON CONFLICT (deal_id) DO UPDATE
                        SET status = 'PENDING',
                            retry_count = 0,
                            next_attempt_at = CURRENT_TIMESTAMP,
                            last_error = NULL,
                            updated_at = CURRENT_TIMESTAMP
                        """,
                dealIds.stream().distinct().map(id -> new Object[]{id}).toList()
        );
    }

    @Override
    public List<DealsSearchIndexRequest> findPending(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, deal_id, retry_count
                        FROM real_estate_deals_search_outbox
                        WHERE status IN ('PENDING', 'FAILED')
                        AND next_attempt_at <= CURRENT_TIMESTAMP
                        ORDER BY id
                        LIMIT ?
                        """,
                (rs, rowNum) -> new DealsSearchIndexRequest(
                        rs.getLong("id"),
                        rs.getLong("deal_id"),
                        rs.getInt("retry_count")
                ),
                Math.max(1, Math.min(10_000, limit))
        );
    }

    @Override
    public void markSucceeded(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                        UPDATE real_estate_deals_search_outbox
                        SET status = 'INDEXED',
                            updated_at = CURRENT_TIMESTAMP,
                            last_error = NULL
                        WHERE id = ?
                        """,
                ids.stream().map(id -> new Object[]{id}).toList()
        );
    }

    @Override
    public void markFailed(Long id, int nextRetryCount, int maxRetries, String errorMessage) {
        String status = nextRetryCount >= maxRetries ? "DEAD" : "FAILED";
        jdbcTemplate.update("""
                        UPDATE real_estate_deals_search_outbox
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
}
