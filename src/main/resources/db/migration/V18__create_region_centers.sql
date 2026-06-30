CREATE TABLE IF NOT EXISTS region_centers (
    id BIGSERIAL PRIMARY KEY,
    region_level VARCHAR(20) NOT NULL,
    region_key VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_region_centers_level_key UNIQUE (region_level, region_key)
);

CREATE INDEX IF NOT EXISTS idx_region_centers_address ON region_centers (address);

INSERT INTO region_centers (region_level, region_key, address, latitude, longitude)
VALUES
('GU', '11680', '서울특별시 강남구', 37.5172, 127.0473),
('GU', '11740', '서울특별시 강동구', 37.5301, 127.1238),
('GU', '11305', '서울특별시 강북구', 37.6396, 127.0257),
('GU', '11500', '서울특별시 강서구', 37.5509, 126.8495),
('GU', '11620', '서울특별시 관악구', 37.4784, 126.9516),
('GU', '11215', '서울특별시 광진구', 37.5384, 127.0823),
('GU', '11530', '서울특별시 구로구', 37.4955, 126.8877),
('GU', '11545', '서울특별시 금천구', 37.4569, 126.8955),
('GU', '11350', '서울특별시 노원구', 37.6542, 127.0568),
('GU', '11320', '서울특별시 도봉구', 37.6688, 127.0471),
('GU', '11230', '서울특별시 동대문구', 37.5744, 127.0396),
('GU', '11590', '서울특별시 동작구', 37.5124, 126.9393),
('GU', '11440', '서울특별시 마포구', 37.5663, 126.9019),
('GU', '11410', '서울특별시 서대문구', 37.5791, 126.9368),
('GU', '11650', '서울특별시 서초구', 37.4837, 127.0324),
('GU', '11200', '서울특별시 성동구', 37.5633, 127.0371),
('GU', '11290', '서울특별시 성북구', 37.5894, 127.0167),
('GU', '11710', '서울특별시 송파구', 37.5145, 127.1059),
('GU', '11470', '서울특별시 양천구', 37.5169, 126.8664),
('GU', '11560', '서울특별시 영등포구', 37.5264, 126.8962),
('GU', '11170', '서울특별시 용산구', 37.5326, 126.9906),
('GU', '11380', '서울특별시 은평구', 37.6027, 126.9291),
('GU', '11110', '서울특별시 종로구', 37.5735, 126.9788),
('GU', '11140', '서울특별시 중구', 37.5638, 126.9976),
('GU', '11260', '서울특별시 중랑구', 37.6063, 127.0925)
ON CONFLICT (region_level, region_key) DO UPDATE
SET address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    updated_at = CURRENT_TIMESTAMP;
