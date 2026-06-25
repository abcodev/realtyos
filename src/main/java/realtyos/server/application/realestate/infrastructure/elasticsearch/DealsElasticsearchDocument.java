package realtyos.server.application.realestate.infrastructure.elasticsearch;

import realtyos.server.application.realestate.domain.Deals;
import realtyos.server.application.realestate.domain.DealsSearchResult;

import java.time.LocalDate;

public record DealsElasticsearchDocument(
        String id,
        String sggCode,
        String umdName,
        String aptName,
        String jibun,
        Integer dealYear,
        Integer dealMonth,
        Integer dealDay,
        String dealDate,
        String dealAmount,
        Long dealAmountValue,
        String exclusiveUseArea,
        Double exclusiveUseAreaValue,
        String floor,
        String buildYear
) {

    static DealsElasticsearchDocument from(Deals deal) {
        return new DealsElasticsearchDocument(
                documentId(deal),
                deal.sggCode(),
                deal.umdName(),
                deal.aptName(),
                deal.jibun(),
                deal.dealYear(),
                deal.dealMonth(),
                deal.dealDay(),
                dealDate(deal),
                deal.dealAmount(),
                parseLong(deal.dealAmount()),
                deal.excluUseArea(),
                parseDouble(deal.excluUseArea()),
                deal.floor(),
                deal.buildYear()
        );
    }

    DealsSearchResult toSearchResult() {
        return new DealsSearchResult(
                id,
                sggCode,
                umdName,
                aptName,
                jibun,
                dealAmount,
                dealAmountValue,
                dealYear,
                dealMonth,
                dealDay,
                dealDate,
                exclusiveUseArea,
                exclusiveUseAreaValue,
                floor,
                buildYear
        );
    }

    private static String documentId(Deals deal) {
        if (deal.id() != null) {
            return String.valueOf(deal.id());
        }
        return String.join(":",
                safe(deal.sggCode()),
                safe(deal.umdName()),
                safe(deal.aptName()),
                safe(deal.jibun()),
                safe(deal.excluUseArea()),
                safe(deal.floor()),
                String.valueOf(deal.dealYear()),
                String.valueOf(deal.dealMonth()),
                String.valueOf(deal.dealDay()),
                safe(deal.dealAmount())
        );
    }

    private static String dealDate(Deals deal) {
        if (deal.dealYear() == null || deal.dealMonth() == null || deal.dealDay() == null) {
            return null;
        }
        return LocalDate.of(deal.dealYear(), deal.dealMonth(), deal.dealDay()).toString();
    }

    private static Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : Long.parseLong(digits);
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
