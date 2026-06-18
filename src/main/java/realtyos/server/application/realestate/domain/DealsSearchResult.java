package realtyos.server.application.realestate.domain;

public record DealsSearchResult(
        String id,
        String sggCode,
        String umdName,
        String aptName,
        String jibun,
        String dealAmount,
        Long dealAmountValue,
        Integer dealYear,
        Integer dealMonth,
        Integer dealDay,
        String dealDate,
        String exclusiveUseArea,
        Double exclusiveUseAreaValue,
        String floor,
        String buildYear
) {
}
