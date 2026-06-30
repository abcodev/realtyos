package realtyos.server.application.realestate.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import realtyos.server.application.realestate.domain.DealsMapGroupLevel;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionCenterSyncService {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient.Builder restClientBuilder;
    private final DealsMapAggregationService mapAggregationService;

    @Value("${app.kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    public RegionCenterSyncResult sync(DealsMapGroupLevel level, int limit) {
        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            throw new IllegalStateException("KAKAO_REST_API_KEY가 설정되지 않았습니다.");
        }
        DealsMapGroupLevel normalizedLevel = level == null ? DealsMapGroupLevel.DONG : level;
        if (normalizedLevel == DealsMapGroupLevel.GU) {
            return new RegionCenterSyncResult(normalizedLevel.name(), 0, 0, 0);
        }

        List<CenterCandidate> candidates = findCandidates(normalizedLevel, limit);
        RestClient restClient = restClientBuilder.build();
        int synced = 0;
        int failed = 0;
        for (CenterCandidate candidate : candidates) {
            Coordinate coordinate = geocode(restClient, candidate.address());
            if (coordinate == null) {
                failed++;
                continue;
            }
            upsert(normalizedLevel, candidate, coordinate);
            synced++;
        }
        if (synced > 0) {
            mapAggregationService.evictMapCache();
        }
        return new RegionCenterSyncResult(normalizedLevel.name(), candidates.size(), synced, failed);
    }

    public void upsert(DealsMapGroupLevel level, String regionKey, String address, Double latitude, Double longitude) {
        if (level == null || regionKey == null || regionKey.isBlank()
                || address == null || address.isBlank() || latitude == null || longitude == null) {
            throw new IllegalArgumentException("좌표 저장 요청이 올바르지 않습니다.");
        }
        upsert(level, new CenterCandidate(regionKey, address), new Coordinate(latitude, longitude));
        mapAggregationService.evictMapCache();
    }

    private List<CenterCandidate> findCandidates(DealsMapGroupLevel level, int limit) {
        String query = switch (level) {
            case DONG -> """
                    SELECT DISTINCT
                        CONCAT_WS(':', d.sgg_code, d.umd_name) AS region_key,
                        CONCAT_WS(' ', s.full_nm, d.umd_name) AS address
                    FROM real_estate_deals d
                    LEFT JOIN real_estate_sgg_code s ON s.sgg_cd = d.sgg_code
                    LEFT JOIN region_centers c ON c.region_level = 'DONG'
                        AND c.region_key = CONCAT_WS(':', d.sgg_code, d.umd_name)
                    WHERE d.umd_name IS NOT NULL
                    AND d.umd_name <> ''
                    AND c.id IS NULL
                    ORDER BY region_key
                    LIMIT ?
                    """;
            case APT -> """
                    SELECT DISTINCT
                        CONCAT_WS(':', d.sgg_code, d.umd_name, d.apt_name, d.jibun) AS region_key,
                        CONCAT_WS(' ', s.full_nm, d.umd_name, d.jibun) AS address
                    FROM real_estate_deals d
                    LEFT JOIN real_estate_sgg_code s ON s.sgg_cd = d.sgg_code
                    LEFT JOIN region_centers c ON c.region_level = 'APT'
                        AND c.region_key = CONCAT_WS(':', d.sgg_code, d.umd_name, d.apt_name, d.jibun)
                    WHERE d.umd_name IS NOT NULL
                    AND d.apt_name IS NOT NULL
                    AND d.jibun IS NOT NULL
                    AND d.umd_name <> ''
                    AND d.apt_name <> ''
                    AND d.jibun <> ''
                    AND c.id IS NULL
                    ORDER BY region_key
                    LIMIT ?
                    """;
            case GU -> throw new IllegalArgumentException("GU는 migration seed를 사용합니다.");
        };
        return jdbcTemplate.query(query, (rs, rowNum) -> new CenterCandidate(
                rs.getString("region_key"),
                rs.getString("address")
        ), Math.max(1, Math.min(1_000, limit)));
    }

    private Coordinate geocode(RestClient restClient, String address) {
        URI uri = UriComponentsBuilder.fromUriString("https://dapi.kakao.com/v2/local/search/address.json")
                .queryParam("query", address)
                .build()
                .encode()
                .toUri();
        JsonNode response = restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                .retrieve()
                .body(JsonNode.class);
        JsonNode documents = response == null ? null : response.path("documents");
        if (documents == null || !documents.isArray() || documents.isEmpty()) {
            return null;
        }
        JsonNode first = documents.get(0);
        return new Coordinate(first.path("y").asDouble(), first.path("x").asDouble());
    }

    private void upsert(DealsMapGroupLevel level, CenterCandidate candidate, Coordinate coordinate) {
        jdbcTemplate.update("""
                INSERT INTO region_centers (region_level, region_key, address, latitude, longitude, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (region_level, region_key) DO UPDATE
                SET address = EXCLUDED.address,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    updated_at = CURRENT_TIMESTAMP
                """,
                level.name(),
                candidate.regionKey(),
                candidate.address(),
                coordinate.latitude(),
                coordinate.longitude()
        );
    }

    private record CenterCandidate(String regionKey, String address) {
    }

    private record Coordinate(double latitude, double longitude) {
    }
}
