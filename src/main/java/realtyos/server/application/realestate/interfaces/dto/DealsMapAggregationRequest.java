package realtyos.server.application.realestate.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import realtyos.server.application.realestate.domain.DealsMapAggregationCondition;
import realtyos.server.application.realestate.domain.DealsMapGroupLevel;

@Schema(description = "지도 실거래 집계 요청")
public record DealsMapAggregationRequest(
        String region,
        DealsMapGroupLevel groupLevel,
        Integer year,
        @Min(1) @Max(12) Integer month,
        Long minPrice,
        Long maxPrice,
        Double minArea,
        Double maxArea,
        @Min(1) @Max(500) Integer limit
) {

    public DealsMapAggregationCondition toCondition() {
        return new DealsMapAggregationCondition(region, groupLevel, year, month, minPrice, maxPrice, minArea, maxArea, limit);
    }
}
