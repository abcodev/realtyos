package realtyos.server.application.realestate.domain;

public record DealsMapAggregationCondition(
        String region,
        DealsMapGroupLevel groupLevel,
        Integer year,
        Integer month,
        Long minPrice,
        Long maxPrice,
        Double minArea,
        Double maxArea,
        Integer limit
) {

    public int normalizedLimit() {
        if (limit == null) {
            return 100;
        }
        return Math.max(1, Math.min(500, limit));
    }

    public DealsMapGroupLevel normalizedGroupLevel() {
        return groupLevel == null ? DealsMapGroupLevel.GU : groupLevel;
    }
}
