package realtyos.server.application.rag.domain;

import java.util.List;

public record ParsedRealestateQuery(
        String originalQuery,
        QueryIntent intent,
        String intentReason,
        RagSearchCondition condition,
        List<QueryTarget> comparisonTargets,
        List<String> comparisonRegions
) {
}
