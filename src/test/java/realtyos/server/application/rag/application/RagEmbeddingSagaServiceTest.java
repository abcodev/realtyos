package realtyos.server.application.rag.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import realtyos.server.application.rag.domain.RagEmbeddingBuildResult;
import realtyos.server.application.rag.domain.RagEmbeddingJobStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RagEmbeddingSagaServiceTest {

    private RagEmbeddingSagaService sagaService;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:rag_saga_%s;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1".formatted(UUID.randomUUID()),
                "sa",
                ""
        ));
        jdbcTemplate.execute("""
                CREATE TABLE rag_embedding_saga (
                    id UUID PRIMARY KEY,
                    status VARCHAR(30) NOT NULL,
                    provider VARCHAR(50) NULL,
                    model VARCHAR(100) NULL,
                    embedding_limit INTEGER NOT NULL,
                    attempt_count INTEGER NOT NULL DEFAULT 0,
                    embedded_count INTEGER NOT NULL DEFAULT 0,
                    skipped_count INTEGER NOT NULL DEFAULT 0,
                    failed_count INTEGER NOT NULL DEFAULT 0,
                    last_error TEXT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP NULL
                )
                """);
        sagaService = new RagEmbeddingSagaService(jdbcTemplate);
    }

    @Test
    void tracksEmbeddingSagaLifecycle() {
        UUID sagaId = sagaService.start(100, "OLLAMA", "nomic-embed-text");

        RagEmbeddingJobStatus requested = sagaService.getJobStatus(sagaId);
        assertThat(requested.status()).isEqualTo("REQUESTED");
        assertThat(requested.embeddingLimit()).isEqualTo(100);

        sagaService.markProcessing(sagaId, 1);
        assertThat(sagaService.getJobStatus(sagaId).status()).isEqualTo("PROCESSING");

        sagaService.markCompleted(sagaId, new RagEmbeddingBuildResult("OLLAMA", "nomic-embed-text", 10, 2, 0));

        RagEmbeddingJobStatus completed = sagaService.getJobStatus(sagaId);
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.embeddedCount()).isEqualTo(10);
        assertThat(completed.skippedCount()).isEqualTo(2);
        assertThat(completed.failedCount()).isZero();
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void findsRecentEmbeddingSagaStatuses() {
        UUID first = sagaService.start(10, "OPENAI", "text-embedding-3-small");
        UUID second = sagaService.start(20, "OLLAMA", "nomic-embed-text");

        List<RagEmbeddingJobStatus> statuses = sagaService.findJobStatuses(10);

        assertThat(statuses)
                .extracting(RagEmbeddingJobStatus::sagaId)
                .contains(first, second);
    }
}
