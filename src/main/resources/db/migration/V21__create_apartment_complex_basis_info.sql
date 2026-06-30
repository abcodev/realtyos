CREATE TABLE IF NOT EXISTS real_estate_apartment_complex_basis_info (
    id BIGSERIAL PRIMARY KEY,
    zipcode VARCHAR(255),
    kapt_code VARCHAR(30) NOT NULL,
    kapt_name VARCHAR(255) NOT NULL,
    kapt_addr VARCHAR(500),
    code_sale_nm VARCHAR(255),
    code_heat_nm VARCHAR(255),
    kapt_tarea NUMERIC(19, 2),
    kapt_dong_cnt INTEGER,
    kaptda_cnt VARCHAR(255),
    kapt_bcompany VARCHAR(255),
    kapt_acompany VARCHAR(255),
    kapt_tel VARCHAR(255),
    kapt_fax VARCHAR(255),
    kapt_url VARCHAR(1000),
    code_apt_nm VARCHAR(255),
    doro_juso VARCHAR(500),
    ho_cnt INTEGER,
    code_mgr_nm VARCHAR(255),
    code_hall_nm VARCHAR(255),
    kapt_usedate VARCHAR(255),
    kapt_marea NUMERIC(19, 2),
    kapt_mparea60 NUMERIC(19, 2),
    kapt_mparea85 NUMERIC(19, 2),
    kapt_mparea135 NUMERIC(19, 2),
    kapt_mparea136 NUMERIC(19, 2),
    priv_area NUMERIC(19, 2),
    bjd_code VARCHAR(20),
    kapt_top_floor INTEGER,
    ktown_flr_no INTEGER,
    kapt_base_floor INTEGER,
    kaptd_ecntp INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_real_estate_apartment_complex_basis_info_kapt_code UNIQUE (kapt_code)
);

CREATE INDEX IF NOT EXISTS idx_real_estate_apartment_complex_basis_info_name
    ON real_estate_apartment_complex_basis_info (kapt_name);

CREATE INDEX IF NOT EXISTS idx_real_estate_apartment_complex_basis_info_bjd_code
    ON real_estate_apartment_complex_basis_info (bjd_code);
