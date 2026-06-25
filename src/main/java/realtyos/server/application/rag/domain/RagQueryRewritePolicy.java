package realtyos.server.application.rag.domain;

import java.util.List;

public class RagQueryRewritePolicy {

    private final RealestateQueryParser queryParser = new RealestateQueryParser();

    public RagQueryRewriteResult rewrite(String query, RagSearchCondition explicitCondition) {
        RagSearchCondition inferred = queryParser.parse(query).condition();
        RagSearchCondition merged = merge(explicitCondition, inferred);
        return new RagQueryRewriteResult(rewriteQuery(query, merged), merged);
    }

    public List<String> inferComparisonRegions(String query) {
        return queryParser.parse(query).comparisonRegions();
    }

    public ParsedRealestateQuery parse(String query) {
        return queryParser.parse(query);
    }

    private String rewriteQuery(String query, RagSearchCondition condition) {
        StringBuilder rewritten = new StringBuilder(query == null ? "" : query.trim());
        if (!containsAny(rewritten.toString(), "실거래", "실거래가")) {
            rewritten.append(" 실거래가");
        }
        if (!containsAny(rewritten.toString(), "아파트")) {
            rewritten.append(" 아파트");
        }
        if (condition.region() != null && !rewritten.toString().contains(condition.region())) {
            rewritten.append(' ').append(condition.region());
        }
        return rewritten.toString().trim();
    }

    private RagSearchCondition merge(RagSearchCondition explicit, RagSearchCondition inferred) {
        if (explicit == null) {
            return inferred;
        }
        return new RagSearchCondition(
                coalesce(explicit.region(), inferred.region()),
                coalesce(explicit.apartmentName(), inferred.apartmentName()),
                coalesce(explicit.fromYear(), inferred.fromYear()),
                coalesce(explicit.fromMonth(), inferred.fromMonth()),
                coalesce(explicit.toYear(), inferred.toYear()),
                coalesce(explicit.toMonth(), inferred.toMonth()),
                coalesce(explicit.minPrice(), inferred.minPrice()),
                coalesce(explicit.maxPrice(), inferred.maxPrice()),
                coalesce(explicit.minArea(), inferred.minArea()),
                coalesce(explicit.maxArea(), inferred.maxArea()),
                coalesce(explicit.recentFirst(), inferred.recentFirst())
        );
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private <T> T coalesce(T explicit, T inferred) {
        if (explicit instanceof String explicitText && explicitText.isBlank()) {
            return inferred;
        }
        return explicit != null ? explicit : inferred;
    }
}
