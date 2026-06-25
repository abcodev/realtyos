package realtyos.server.application.realestate.domain;

public record DealsSearchCondition(
        String region,
        String apartmentName,
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
            return 20;
        }
        return Math.max(1, Math.min(100, limit));
    }
}
