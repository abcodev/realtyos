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
