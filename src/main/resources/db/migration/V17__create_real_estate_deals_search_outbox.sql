CREATE TABLE IF NOT EXISTS real_estate_deals_search_outbox (
    id BIGSERIAL PRIMARY KEY,
    deal_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_real_estate_deals_search_outbox_pending
    ON real_estate_deals_search_outbox (status, next_attempt_at, id);
