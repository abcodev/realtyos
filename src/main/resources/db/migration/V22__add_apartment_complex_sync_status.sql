ALTER TABLE real_estate_apartment_complex
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE real_estate_apartment_complex_basis_info
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_real_estate_apartment_complex_active
    ON real_estate_apartment_complex (active);

CREATE INDEX IF NOT EXISTS idx_real_estate_apartment_complex_basis_info_active
    ON real_estate_apartment_complex_basis_info (active);
