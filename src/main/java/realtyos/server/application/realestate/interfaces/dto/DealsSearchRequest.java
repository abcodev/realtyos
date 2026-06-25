package realtyos.server.application.realestate.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import realtyos.server.application.realestate.domain.DealsSearchCondition;

@Schema(description = "실거래 Elasticsearch 검색 요청")
public record DealsSearchRequest(
        String region,
        String apartmentName,
        Integer year,
        @Min(1) @Max(12) Integer month,
        Long minPrice,
        Long maxPrice,
        Double minArea,
        Double maxArea,
        @Min(1) @Max(100) Integer limit
) {

    public DealsSearchCondition toCondition() {
        return new DealsSearchCondition(region, apartmentName, year, month, minPrice, maxPrice, minArea, maxArea, limit);
    }
}
