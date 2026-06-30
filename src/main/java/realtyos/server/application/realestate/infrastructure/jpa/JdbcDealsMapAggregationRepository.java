package realtyos.server.application.realestate.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import realtyos.server.application.realestate.domain.DealsMapAggregation;
import realtyos.server.application.realestate.domain.DealsMapAggregationCondition;
import realtyos.server.application.realestate.domain.DealsMapAggregationRepository;
import realtyos.server.application.realestate.domain.DealsMapGroupLevel;
import realtyos.server.application.realestate.domain.RegionResolution;
import realtyos.server.application.realestate.domain.RegionResolutionType;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcDealsMapAggregationRepository implements DealsMapAggregationRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final Map<String, String> SIDO_CODE_PREFIXES = Map.ofEntries(
            Map.entry("서울", "11"),
            Map.entry("서울시", "11"),
            Map.entry("서울특별시", "11"),
            Map.entry("부산", "26"),
            Map.entry("부산시", "26"),
            Map.entry("부산광역시", "26"),
            Map.entry("대구", "27"),
            Map.entry("대구광역시", "27"),
            Map.entry("인천", "28"),
            Map.entry("인천광역시", "28"),
            Map.entry("광주", "29"),
            Map.entry("광주광역시", "29"),
            Map.entry("대전", "30"),
            Map.entry("대전광역시", "30"),
            Map.entry("울산", "31"),
            Map.entry("울산광역시", "31"),
            Map.entry("세종", "36"),
            Map.entry("세종시", "36"),
            Map.entry("세종특별자치시", "36"),
            Map.entry("경기", "41"),
            Map.entry("경기도", "41"),
            Map.entry("강원", "51"),
            Map.entry("강원도", "51"),
            Map.entry("강원특별자치도", "51"),
            Map.entry("충북", "43"),
            Map.entry("충청북도", "43"),
            Map.entry("충남", "44"),
            Map.entry("충청남도", "44"),
            Map.entry("전북", "52"),
            Map.entry("전라북도", "52"),
            Map.entry("전북특별자치도", "52"),
            Map.entry("전남", "46"),
            Map.entry("전라남도", "46"),
            Map.entry("경북", "47"),
            Map.entry("경상북도", "47"),
            Map.entry("경남", "48"),
            Map.entry("경상남도", "48"),
            Map.entry("제주", "50"),
            Map.entry("제주도", "50"),
            Map.entry("제주특별자치도", "50")
    );
    private static final Map<String, String> SIDO_PREFIX_NAMES = Map.ofEntries(
            Map.entry("11", "서울특별시"),
            Map.entry("26", "부산광역시"),
            Map.entry("27", "대구광역시"),
            Map.entry("28", "인천광역시"),
            Map.entry("29", "광주광역시"),
            Map.entry("30", "대전광역시"),
            Map.entry("31", "울산광역시"),
            Map.entry("36", "세종특별자치시"),
            Map.entry("41", "경기도"),
            Map.entry("51", "강원특별자치도"),
            Map.entry("43", "충청북도"),
            Map.entry("44", "충청남도"),
            Map.entry("52", "전북특별자치도"),
            Map.entry("46", "전라남도"),
            Map.entry("47", "경상북도"),
            Map.entry("48", "경상남도"),
            Map.entry("50", "제주특별자치도")
    );

    @Override
    public List<DealsMapAggregation> aggregate(DealsMapAggregationCondition condition, RegionResolution regionResolution) {
        DealsMapGroupLevel level = condition.normalizedGroupLevel();
        GroupColumns columns = GroupColumns.of(level);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE d.deal_amount IS NOT NULL AND d.deal_amount <> ''");

        appendSidoFilter(where, params, condition.region());
        appendRegionFilter(where, params, regionResolution);
        appendConditionFilters(where, params, condition);

        String sql = """
                WITH filtered AS (
                    SELECT
                        d.sgg_code,
                        d.umd_name,
                        d.apt_name,
                        d.jibun,
                        s.sig_kor_nm AS sgg_name,
                        s.full_nm AS sgg_full_name,
                        NULLIF(REGEXP_REPLACE(COALESCE(d.deal_amount, ''), '[^0-9]', '', 'g'), '')::BIGINT AS amount_value,
                        NULLIF(REGEXP_REPLACE(COALESCE(d.exclu_use_area, ''), '[^0-9.]', '', 'g'), '')::DOUBLE PRECISION AS area_value,
                        CASE
                            WHEN d.deal_year IS NOT NULL AND d.deal_month BETWEEN 1 AND 12 AND d.deal_day BETWEEN 1 AND 31
                            THEN MAKE_DATE(d.deal_year, d.deal_month, LEAST(d.deal_day, 28))
                            ELSE NULL
                        END AS deal_date
                    FROM real_estate_deals d
                    LEFT JOIN real_estate_sgg_code s ON s.sgg_cd = d.sgg_code
                """ + where + """
                ),
                aggregated AS (
                    SELECT
                        %s AS group_key,
                        %s AS label,
                        %s AS address,
                        MAX(sgg_code) AS sgg_code,
                        MAX(sgg_name) AS sgg_name,
                        MAX(umd_name) AS umd_name,
                        MAX(apt_name) AS apt_name,
                        MAX(jibun) AS jibun,
                        COUNT(*) AS deal_count,
                        ROUND(AVG(amount_value))::BIGINT AS average_deal_amount,
                        MIN(amount_value) AS min_deal_amount,
                        MAX(amount_value) AS max_deal_amount,
                        ROUND(AVG(area_value)::NUMERIC, 2)::DOUBLE PRECISION AS average_exclusive_area,
                        MAX(deal_date) AS latest_deal_date
                    FROM filtered
                    WHERE amount_value IS NOT NULL
                    GROUP BY %s
                    ORDER BY deal_count DESC, average_deal_amount DESC NULLS LAST
                    LIMIT ?
                ),
                centered AS (
                    SELECT
                        a.*,
                        c.latitude,
                        c.longitude
                    FROM aggregated a
                    LEFT JOIN region_centers c
                        ON c.region_level = ?
                        AND c.region_key = a.group_key
                )
                SELECT *
                FROM centered
                """.formatted(columns.groupKey(), columns.label(), columns.address(), columns.groupBy());
        params.add(condition.normalizedLimit());
        params.add(level.name());

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DealsMapAggregation(
                rs.getString("group_key"),
                level,
                rs.getString("label"),
                rs.getString("address"),
                rs.getString("sgg_code"),
                rs.getString("sgg_name"),
                rs.getString("umd_name"),
                rs.getString("apt_name"),
                rs.getString("jibun"),
                rs.getLong("deal_count"),
                getLong(rs.getObject("average_deal_amount")),
                getLong(rs.getObject("min_deal_amount")),
                getLong(rs.getObject("max_deal_amount")),
                getDouble(rs.getObject("average_exclusive_area")),
                toLocalDate(rs.getDate("latest_deal_date")),
                getDouble(rs.getObject("latitude")),
                getDouble(rs.getObject("longitude"))
        ), params.toArray());
    }

    @Override
    public String resolveRegionByCenter(double latitude, double longitude, DealsMapGroupLevel groupLevel) {
        if (groupLevel == DealsMapGroupLevel.GU) {
            String sidoName = resolveSidoByCoordinate(latitude, longitude);
            if (sidoName != null) {
                return sidoName;
            }
        }

        String sql = """
                SELECT
                    s.full_nm AS full_name,
                    c.region_key AS region_key
                FROM region_centers c
                LEFT JOIN real_estate_sgg_code s
                    ON s.sgg_cd = CASE
                        WHEN c.region_level = 'GU' THEN c.region_key
                        ELSE SPLIT_PART(c.region_key, ':', 1)
                    END
                WHERE c.latitude IS NOT NULL
                    AND c.longitude IS NOT NULL
                    AND c.region_level = 'GU'
                ORDER BY
                    POWER(c.latitude - ?, 2)
                    + POWER((c.longitude - ?) * COS(RADIANS(?)), 2)
                LIMIT 1
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, latitude, longitude, latitude);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        String fullName = row.get("full_name") == null ? null : String.valueOf(row.get("full_name"));
        if (groupLevel != DealsMapGroupLevel.GU && fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        String regionKey = row.get("region_key") == null ? null : String.valueOf(row.get("region_key"));
        if (regionKey == null || regionKey.length() < 2) {
            return null;
        }
        return SIDO_PREFIX_NAMES.get(regionKey.substring(0, 2));
    }

    private void appendRegionFilter(StringBuilder where, List<Object> params, RegionResolution regionResolution) {
        if (regionResolution == null || !regionResolution.hasFilter()) {
            return;
        }
        if (regionResolution.type() == RegionResolutionType.SGG && !regionResolution.sggCodes().isEmpty()) {
            where.append(" AND d.sgg_code IN (");
            where.append("?,".repeat(regionResolution.sggCodes().size()));
            where.setLength(where.length() - 1);
            where.append(")");
            params.addAll(regionResolution.sggCodes());
            return;
        }
        if (regionResolution.type() == RegionResolutionType.DONG) {
            where.append(" AND d.umd_name = ?");
            params.add(regionResolution.dongName());
            return;
        }
        if (regionResolution.type() == RegionResolutionType.KEYWORD) {
            where.append(" AND (d.umd_name ILIKE ? OR d.apt_name ILIKE ?)");
            String keyword = "%" + regionResolution.keyword() + "%";
            params.add(keyword);
            params.add(keyword);
        }
    }

    private void appendSidoFilter(StringBuilder where, List<Object> params, String region) {
        if (region == null || region.isBlank()) {
            return;
        }
        String normalized = region.trim();
        String prefix = SIDO_CODE_PREFIXES.get(normalized);
        if (prefix != null) {
            where.append(" AND d.sgg_code LIKE ?");
            params.add(prefix + "%");
        }
    }

    private String resolveSidoByCoordinate(double latitude, double longitude) {
        if (inside(latitude, longitude, 37.42, 37.72, 126.76, 127.19)) return "서울특별시";
        if (inside(latitude, longitude, 37.00, 37.98, 124.60, 126.90)) return "인천광역시";
        if (inside(latitude, longitude, 36.75, 38.35, 126.35, 127.95)) return "경기도";
        if (inside(latitude, longitude, 35.00, 35.45, 128.75, 129.35)) return "부산광역시";
        if (inside(latitude, longitude, 35.75, 36.05, 128.35, 128.80)) return "대구광역시";
        if (inside(latitude, longitude, 37.25, 37.65, 126.35, 126.85)) return "인천광역시";
        if (inside(latitude, longitude, 35.05, 35.30, 126.65, 127.05)) return "광주광역시";
        if (inside(latitude, longitude, 36.15, 36.55, 127.20, 127.55)) return "대전광역시";
        if (inside(latitude, longitude, 35.35, 35.75, 128.95, 129.50)) return "울산광역시";
        if (inside(latitude, longitude, 36.35, 36.75, 127.05, 127.45)) return "세종특별자치시";
        if (inside(latitude, longitude, 37.00, 38.65, 127.05, 129.35)) return "강원특별자치도";
        if (inside(latitude, longitude, 36.45, 37.35, 127.25, 128.75)) return "충청북도";
        if (inside(latitude, longitude, 35.95, 37.05, 126.05, 127.65)) return "충청남도";
        if (inside(latitude, longitude, 35.30, 36.20, 126.35, 127.95)) return "전북특별자치도";
        if (inside(latitude, longitude, 33.90, 35.45, 125.00, 127.90)) return "전라남도";
        if (inside(latitude, longitude, 35.55, 37.60, 128.00, 130.00)) return "경상북도";
        if (inside(latitude, longitude, 34.55, 35.95, 127.55, 129.60)) return "경상남도";
        if (inside(latitude, longitude, 33.05, 33.65, 126.05, 126.95)) return "제주특별자치도";
        return null;
    }

    private boolean inside(double latitude, double longitude, double minLat, double maxLat, double minLng, double maxLng) {
        return latitude >= minLat && latitude <= maxLat && longitude >= minLng && longitude <= maxLng;
    }

    private void appendConditionFilters(StringBuilder where, List<Object> params, DealsMapAggregationCondition condition) {
        if (condition.year() != null) {
            where.append(" AND d.deal_year = ?");
            params.add(condition.year());
        }
        if (condition.month() != null) {
            where.append(" AND d.deal_month = ?");
            params.add(condition.month());
        }
        appendAmountFilter(where, params, ">=", condition.minPrice());
        appendAmountFilter(where, params, "<=", condition.maxPrice());
        appendAreaFilter(where, params, ">=", condition.minArea());
        appendAreaFilter(where, params, "<=", condition.maxArea());
    }

    private void appendAmountFilter(StringBuilder where, List<Object> params, String operator, Long value) {
        if (value == null) {
            return;
        }
        where.append(" AND NULLIF(REGEXP_REPLACE(COALESCE(d.deal_amount, ''), '[^0-9]', '', 'g'), '')::BIGINT ")
                .append(operator)
                .append(" ?");
        params.add(value);
    }

    private void appendAreaFilter(StringBuilder where, List<Object> params, String operator, Double value) {
        if (value == null) {
            return;
        }
        where.append(" AND NULLIF(REGEXP_REPLACE(COALESCE(d.exclu_use_area, ''), '[^0-9.]', '', 'g'), '')::DOUBLE PRECISION ")
                .append(operator)
                .append(" ?");
        params.add(value);
    }

    private Long getLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Double getDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private java.time.LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private record GroupColumns(String groupKey, String label, String address, String groupBy) {

        private static GroupColumns of(DealsMapGroupLevel level) {
            return switch (level) {
                case GU -> new GroupColumns(
                        "COALESCE(sgg_code, '')",
                        "COALESCE(sgg_name, sgg_code, '지역 미상')",
                        "COALESCE(sgg_full_name, sgg_name, sgg_code, '서울특별시')",
                        "sgg_code, sgg_name, sgg_full_name"
                );
                case DONG -> new GroupColumns(
                        "CONCAT_WS(':', sgg_code, umd_name)",
                        "COALESCE(umd_name, '동 미상')",
                        "CONCAT_WS(' ', sgg_full_name, umd_name)",
                        "sgg_code, sgg_full_name, umd_name"
                );
                case APT -> new GroupColumns(
                        "CONCAT_WS(':', sgg_code, umd_name, apt_name, jibun)",
                        "COALESCE(apt_name, '단지 미상')",
                        "CONCAT_WS(' ', sgg_full_name, umd_name, jibun)",
                        "sgg_code, sgg_full_name, umd_name, apt_name, jibun"
                );
            };
        }
    }
}
