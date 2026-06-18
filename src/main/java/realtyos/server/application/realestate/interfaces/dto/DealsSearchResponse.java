package realtyos.server.application.realestate.interfaces.dto;

import realtyos.server.application.realestate.domain.DealsSearchResult;

public record DealsSearchResponse(
        String id,
        String sggCode,
        String umdName,
        String aptName,
        String jibun,
        String dealAmount,
        Integer dealYear,
        Integer dealMonth,
        Integer dealDay,
        String exclusiveUseArea,
        String floor,
        String buildYear
) {

    public static DealsSearchResponse from(DealsSearchResult result) {
        return new DealsSearchResponse(
                result.id(),
                result.sggCode(),
                result.umdName(),
                result.aptName(),
                result.jibun(),
                result.dealAmount(),
                result.dealYear(),
                result.dealMonth(),
                result.dealDay(),
                result.exclusiveUseArea(),
                result.floor(),
                result.buildYear()
        );
    }
}
