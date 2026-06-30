package realtyos.server.application.realestate.interfaces.dto;

import realtyos.server.application.realestate.domain.DealsMapAggregation;
import realtyos.server.application.realestate.domain.DealsMapGroupLevel;

import java.time.LocalDate;

public record DealsMapAggregationResponse(
        String groupKey,
        DealsMapGroupLevel groupLevel,
        String label,
        String address,
        String sggCode,
        String sggName,
        String umdName,
        String aptName,
        String jibun,
        long dealCount,
        Long averageDealAmount,
        Long minDealAmount,
        Long maxDealAmount,
        Double averageExclusiveArea,
        LocalDate latestDealDate,
        Double latitude,
        Double longitude
) {

    public static DealsMapAggregationResponse from(DealsMapAggregation aggregation) {
        return new DealsMapAggregationResponse(
                aggregation.groupKey(),
                aggregation.groupLevel(),
                aggregation.label(),
                aggregation.address(),
                aggregation.sggCode(),
                aggregation.sggName(),
                aggregation.umdName(),
                aggregation.aptName(),
                aggregation.jibun(),
                aggregation.dealCount(),
                aggregation.averageDealAmount(),
                aggregation.minDealAmount(),
                aggregation.maxDealAmount(),
                aggregation.averageExclusiveArea(),
                aggregation.latestDealDate(),
                aggregation.latitude(),
                aggregation.longitude()
        );
    }
}
