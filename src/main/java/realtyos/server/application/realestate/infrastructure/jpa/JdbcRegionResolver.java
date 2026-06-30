package realtyos.server.application.realestate.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import realtyos.server.application.realestate.domain.RegionResolution;
import realtyos.server.application.realestate.domain.RegionResolver;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JdbcRegionResolver implements RegionResolver {

    private final JdbcTemplate jdbcTemplate;
    private static final Set<String> SIDO_LEVEL_REGIONS = Set.of(
            "서울", "서울시", "서울특별시",
            "부산", "부산시", "부산광역시",
            "대구", "대구시", "대구광역시",
            "인천", "인천시", "인천광역시",
            "광주", "광주시", "광주광역시",
            "대전", "대전시", "대전광역시",
            "울산", "울산시", "울산광역시",
            "세종", "세종시", "세종특별자치시",
            "경기", "경기도",
            "강원", "강원도", "강원특별자치도",
            "충북", "충청북도",
            "충남", "충청남도",
            "전북", "전라북도", "전북특별자치도",
            "전남", "전라남도",
            "경북", "경상북도",
            "경남", "경상남도",
            "제주", "제주도", "제주특별자치도"
    );

    @Override
    public RegionResolution resolve(String region) {
        if (region == null || region.isBlank()) {
            return RegionResolution.empty(region);
        }

        String normalized = region.trim();
        if (isSidoLevelRegion(normalized)) {
            return RegionResolution.empty(normalized);
        }
        if (isDongLevelRegion(normalized)) {
            return RegionResolution.dong(normalized, normalized);
        }

        List<String> sggCodes = findSggCodes(normalized);
        if (!sggCodes.isEmpty()) {
            return RegionResolution.sgg(normalized, sggCodes);
        }
        return RegionResolution.keyword(normalized, normalized);
    }

    private List<String> findSggCodes(String region) {
        return jdbcTemplate.queryForList("""
                SELECT s.sgg_cd
                FROM real_estate_sgg_code s
                WHERE s.sig_kor_nm IN (?, ?)
                OR s.full_nm ILIKE ?
                OR s.full_nm ILIKE ?
                ORDER BY s.sgg_cd
                """, String.class, region, region + "구", "% " + region + "구", "%" + region + "%");
    }

    private boolean isDongLevelRegion(String value) {
        return value.endsWith("동") || value.endsWith("읍") || value.endsWith("면") || value.endsWith("리");
    }

    private boolean isSidoLevelRegion(String value) {
        return SIDO_LEVEL_REGIONS.contains(value)
                || value.endsWith("광역시")
                || value.endsWith("특별시")
                || value.endsWith("특별자치시")
                || value.endsWith("특별자치도")
                || value.endsWith("도");
    }
}
