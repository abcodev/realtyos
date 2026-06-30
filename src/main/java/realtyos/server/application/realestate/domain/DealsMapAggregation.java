package realtyos.server.application.realestate.domain;

import java.time.LocalDate;

public record DealsMapAggregation(
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
}
