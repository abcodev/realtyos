CREATE TABLE IF NOT EXISTS real_estate_apartment_complex (
    id BIGSERIAL PRIMARY KEY,
    kapt_code VARCHAR(30) NOT NULL,
    kapt_name VARCHAR(255) NOT NULL,
    as1 VARCHAR(100),
    as2 VARCHAR(100),
    as3 VARCHAR(100),
    as4 VARCHAR(100),
    bjd_code VARCHAR(20),
    full_address VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_real_estate_apartment_complex_kapt_code UNIQUE (kapt_code)
);

CREATE INDEX IF NOT EXISTS idx_real_estate_apartment_complex_name
    ON real_estate_apartment_complex (kapt_name);

CREATE INDEX IF NOT EXISTS idx_real_estate_apartment_complex_bjd_code
    ON real_estate_apartment_complex (bjd_code);
