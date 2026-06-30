package realtyos.server.application.realestate.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import realtyos.server.application.common.response.ApiResponse;
import realtyos.server.application.realestate.application.service.DealsSearchIndexService;
import realtyos.server.application.realestate.application.service.DealsSearchReindexResult;
import realtyos.server.application.realestate.application.service.DealsSearchService;
import realtyos.server.application.realestate.application.service.DealsMapAggregationService;
import realtyos.server.application.realestate.application.service.RegionCenterSyncService;
import realtyos.server.application.realestate.domain.DealsMapGroupLevel;
import realtyos.server.application.realestate.interfaces.dto.DealsMapAggregationRequest;
import realtyos.server.application.realestate.interfaces.dto.DealsMapAggregationResponse;
import realtyos.server.application.realestate.interfaces.dto.DealsReindexResponse;
import realtyos.server.application.realestate.interfaces.dto.DealsSearchRequest;
import realtyos.server.application.realestate.interfaces.dto.DealsSearchResponse;
import realtyos.server.application.realestate.interfaces.dto.RegionCenterSyncResponse;
import realtyos.server.application.realestate.interfaces.dto.RegionCenterUpsertRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/realestate/deals")
@Tag(name = "Realestate Deals", description = "부동산 실거래 검색 API")
public class DealsSearchController {

    private final DealsSearchService searchService;
    private final DealsSearchIndexService indexService;
    private final DealsMapAggregationService mapAggregationService;
    private final RegionCenterSyncService regionCenterSyncService;

    @PostMapping("/search")
    @Operation(summary = "실거래 지역/년도 검색", description = "Elasticsearch에서 지역, 연도, 월, 단지명, 금액/면적 조건으로 실거래를 조회합니다.")
    public ApiResponse<List<DealsSearchResponse>> search(@RequestBody @Valid DealsSearchRequest request) {
        return ApiResponse.success(searchService.search(request.toCondition()).stream()
                .map(DealsSearchResponse::from)
                .toList());
    }

    @PostMapping("/map/aggregates")
    @Operation(summary = "지도 실거래 집계", description = "카카오맵 줌 레벨에 맞춰 구, 동, 아파트 단위로 실거래를 집계합니다.")
    public ApiResponse<List<DealsMapAggregationResponse>> mapAggregates(@RequestBody @Valid DealsMapAggregationRequest request) {
        return ApiResponse.success(mapAggregationService.aggregate(request.toCondition()).stream()
                .map(DealsMapAggregationResponse::from)
                .toList());
    }

    @PostMapping("/map/centers/sync")
    @Operation(summary = "지도 중심 좌표 동기화", description = "누락된 동/아파트 주소 중심 좌표를 Kakao Local API로 채워 region_centers에 저장합니다.")
    public ApiResponse<RegionCenterSyncResponse> syncMapCenters(
            @RequestParam(defaultValue = "DONG") DealsMapGroupLevel level,
            @RequestParam(defaultValue = "100") Integer limit
    ) {
        return ApiResponse.success(RegionCenterSyncResponse.from(
                regionCenterSyncService.sync(level, limit == null ? 100 : limit)
        ));
    }

    @PostMapping("/map/centers")
    @Operation(summary = "지도 중심 좌표 저장", description = "프론트 geocoder가 얻은 구/동/아파트 중심 좌표를 region_centers에 저장합니다.")
    public ApiResponse<Void> upsertMapCenter(@RequestBody @Valid RegionCenterUpsertRequest request) {
        regionCenterSyncService.upsert(
                request.regionLevel(),
                request.regionKey(),
                request.address(),
                request.latitude(),
                request.longitude()
        );
        return ApiResponse.empty();
    }

    @PostMapping("/search/reindex")
    @Operation(summary = "실거래 Elasticsearch 재색인", description = "PostgreSQL 실거래 데이터를 Elasticsearch 검색 인덱스로 업서트합니다.")
    public ApiResponse<DealsReindexResponse> reindex(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) Integer batchSize,
            @RequestParam(required = false) Integer maxBatches
    ) {
        DealsSearchReindexResult result = indexService.reindex(year, afterId, batchSize, maxBatches);
        return ApiResponse.success(DealsReindexResponse.from(result));
    }
}
