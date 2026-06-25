package realtyos.server.application.realestate.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import realtyos.server.application.rag.domain.ParsedRealestateQuery;
import realtyos.server.application.rag.domain.QueryTarget;
import realtyos.server.application.rag.domain.QueryTargetKind;
import realtyos.server.application.rag.domain.RagQueryRewritePolicy;
import realtyos.server.application.rag.domain.RagSearchCondition;
import realtyos.server.application.realestate.domain.DecisionCandidate;
import realtyos.server.application.realestate.domain.DecisionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealestateDecisionService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final DealAnalysisService dealAnalysisService;
    private final DecisionScoreService decisionScoreService;
    private final DecisionTargetSummaryBuilder targetSummaryBuilder;
    private final DecisionResultFormatter decisionResultFormatter;
    private final RagQueryRewritePolicy queryRewritePolicy = new RagQueryRewritePolicy();

    public DecisionResult decide(String query, Integer limit, RagSearchCondition explicitCondition) {
        ParsedRealestateQuery parsedQuery = queryRewritePolicy.parse(query);
        List<QueryTarget> comparisonTargets = parsedQuery.comparisonTargets();
        List<String> comparisonRegions = parsedQuery.comparisonRegions();
        boolean hasComparisonTargets = !comparisonTargets.isEmpty();
        RagSearchCondition condition = queryRewritePolicy
                .rewrite(query, !hasComparisonTargets && comparisonRegions.isEmpty() ? explicitCondition : withoutRegionAndApartment(explicitCondition))
                .condition();
        if (condition.region() == null && containsSeoul(query)) {
            condition = withRegion(condition, "서울");
        }
        RagSearchCondition resolvedCondition = condition;

        List<DecisionCandidate> rawCandidates = hasComparisonTargets
                ? findComparisonTargetCandidates(comparisonTargets, resolvedCondition, normalizeLimit(limit))
                : comparisonRegions.isEmpty()
                ? dealAnalysisService.findCandidates(resolvedCondition, normalizeLimit(limit))
                : findComparisonCandidates(comparisonRegions, resolvedCondition, normalizeLimit(limit));
        List<DecisionCandidate> scoredCandidates = rawCandidates.stream()
                .map(candidate -> decisionScoreService.score(candidate, resolvedCondition))
                .sorted(Comparator.comparing(DecisionCandidate::score).reversed()
                        .thenComparing(DecisionCandidate::dealCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<DecisionCandidate> candidates = hasComparisonTargets
                ? balanceComparisonTargetCandidates(scoredCandidates, comparisonTargets, normalizeLimit(limit))
                : comparisonRegions.isEmpty()
                ? scoredCandidates
                : balanceComparisonCandidates(scoredCandidates, comparisonRegions, normalizeLimit(limit));

        return new DecisionResult(
                query,
                resolvedCondition,
                buildSummary(candidates, comparisonTargets),
                comparisonTargets.stream().map(QueryTarget::value).toList(),
                targetSummaryBuilder.build(candidates, comparisonTargets.stream().map(QueryTarget::value).toList()),
                candidates
        );
    }

    private List<DecisionCandidate> findComparisonCandidates(
            List<String> comparisonRegions,
            RagSearchCondition baseCondition,
            int limit
    ) {
        Map<String, DecisionCandidate> merged = new LinkedHashMap<>();
        int perRegionLimit = Math.max(3, Math.min(8, limit));
        for (String region : comparisonRegions) {
            RagSearchCondition regionCondition = withRegion(baseCondition, region);
            dealAnalysisService.findCandidates(regionCondition, perRegionLimit)
                    .forEach(candidate -> merged.putIfAbsent(candidateKey(candidate), candidate));
        }
        return new ArrayList<>(merged.values());
    }

    private List<DecisionCandidate> findComparisonTargetCandidates(
            List<QueryTarget> targets,
            RagSearchCondition baseCondition,
            int limit
    ) {
        Map<String, DecisionCandidate> merged = new LinkedHashMap<>();
        int perTargetLimit = Math.max(3, Math.min(8, limit));
        for (QueryTarget target : targets) {
            findTargetCandidates(target, baseCondition, perTargetLimit)
                    .forEach(candidate -> merged.putIfAbsent(candidateKey(candidate), candidate));
        }
        return new ArrayList<>(merged.values());
    }

    private List<DecisionCandidate> findTargetCandidates(
            QueryTarget target,
            RagSearchCondition baseCondition,
            int limit
    ) {
        if (target.kind() != QueryTargetKind.ANY) {
            return dealAnalysisService.findCandidates(toCondition(target, baseCondition), limit);
        }

        List<DecisionCandidate> regionCandidates = dealAnalysisService.findCandidates(
                regionCondition(target.value(), baseCondition),
                limit
        );
        if (!regionCandidates.isEmpty()) {
            return regionCandidates;
        }
        return dealAnalysisService.findCandidates(apartmentCondition(target.value(), baseCondition), limit);
    }

    private List<DecisionCandidate> balanceComparisonCandidates(
            List<DecisionCandidate> candidates,
            List<String> comparisonRegions,
            int limit
    ) {
        LinkedHashMap<String, DecisionCandidate> balanced = new LinkedHashMap<>();
        int perRegionLimit = Math.max(2, Math.min(3, limit));

        for (String region : comparisonRegions) {
            candidates.stream()
                    .filter(candidate -> matchesRegion(candidate, region))
                    .limit(perRegionLimit)
                    .forEach(candidate -> balanced.putIfAbsent(candidateKey(candidate), candidate));
        }

        candidates.stream()
                .limit(limit)
                .forEach(candidate -> balanced.putIfAbsent(candidateKey(candidate), candidate));

        return new ArrayList<>(balanced.values()).stream()
                .limit(limit)
                .toList();
    }

    private boolean matchesRegion(DecisionCandidate candidate, String region) {
        String dongName = candidate.dongName() == null ? "" : candidate.dongName();
        return dongName.contains(region) || region.contains(dongName);
    }

    private List<DecisionCandidate> balanceComparisonTargetCandidates(
            List<DecisionCandidate> candidates,
            List<QueryTarget> targets,
            int limit
    ) {
        LinkedHashMap<String, DecisionCandidate> balanced = new LinkedHashMap<>();
        int perTargetLimit = Math.max(2, Math.min(3, limit));

        for (QueryTarget target : targets) {
            candidates.stream()
                    .filter(candidate -> matchesTarget(candidate, target))
                    .limit(perTargetLimit)
                    .forEach(candidate -> balanced.putIfAbsent(candidateKey(candidate), candidate));
        }

        return new ArrayList<>(balanced.values()).stream()
                .limit(limit)
                .toList();
    }

    private String candidateKey(DecisionCandidate candidate) {
        return "%s:%s:%s".formatted(candidate.regionCode(), candidate.dongName(), candidate.apartmentName());
    }

    public String formatAnswer(DecisionResult result) {
        return decisionResultFormatter.format(result);
    }

    private String buildSummary(List<DecisionCandidate> candidates, List<QueryTarget> comparisonTargets) {
        if (candidates.isEmpty()) {
            return "조건에 맞는 최근 실거래 후보를 찾지 못했습니다.";
        }
        if (comparisonTargets.size() > 1) {
            return "%s 비교 결과입니다. 각 대상별 최근 실거래 후보를 분리해 점수화했습니다."
                    .formatted(String.join(" / ", comparisonTargets.stream().map(QueryTarget::value).toList()));
        }
        DecisionCandidate best = candidates.getFirst();
        return "%s %s가 현재 조건에서 가장 높은 점수(%.1f점)를 받았습니다. 최근 거래, 예산 적합도, 면적 적합도, 거래 건수를 함께 고려한 결과입니다."
                .formatted(best.dongName(), best.apartmentName(), best.score());
    }

    private RagSearchCondition withRegion(RagSearchCondition condition, String region) {
        return new RagSearchCondition(
                region,
                condition == null ? null : condition.apartmentName(),
                condition == null ? null : condition.fromYear(),
                condition == null ? null : condition.fromMonth(),
                condition == null ? null : condition.toYear(),
                condition == null ? null : condition.toMonth(),
                condition == null ? null : condition.minPrice(),
                condition == null ? null : condition.maxPrice(),
                condition == null ? null : condition.minArea(),
                condition == null ? null : condition.maxArea(),
                condition == null ? null : condition.recentFirst()
        );
    }

    private RagSearchCondition toCondition(QueryTarget target, RagSearchCondition base) {
        return target.kind() == QueryTargetKind.REGION
                ? regionCondition(target.value(), base)
                : apartmentCondition(target.value(), base);
    }

    private RagSearchCondition regionCondition(String region, RagSearchCondition base) {
        return new RagSearchCondition(
                region,
                base == null ? null : base.apartmentName(),
                base == null ? null : base.fromYear(),
                base == null ? null : base.fromMonth(),
                base == null ? null : base.toYear(),
                base == null ? null : base.toMonth(),
                base == null ? null : base.minPrice(),
                base == null ? null : base.maxPrice(),
                base == null ? null : base.minArea(),
                base == null ? null : base.maxArea(),
                base == null ? null : base.recentFirst()
        );
    }

    private RagSearchCondition apartmentCondition(String apartmentName, RagSearchCondition base) {
        return new RagSearchCondition(
                base == null ? null : base.region(),
                apartmentName,
                base == null ? null : base.fromYear(),
                base == null ? null : base.fromMonth(),
                base == null ? null : base.toYear(),
                base == null ? null : base.toMonth(),
                base == null ? null : base.minPrice(),
                base == null ? null : base.maxPrice(),
                base == null ? null : base.minArea(),
                base == null ? null : base.maxArea(),
                base == null ? null : base.recentFirst()
        );
    }

    private RagSearchCondition withoutRegionAndApartment(RagSearchCondition condition) {
        if (condition == null) {
            return null;
        }
        return new RagSearchCondition(
                null,
                null,
                condition.fromYear(),
                condition.fromMonth(),
                condition.toYear(),
                condition.toMonth(),
                condition.minPrice(),
                condition.maxPrice(),
                condition.minArea(),
                condition.maxArea(),
                condition.recentFirst()
        );
    }

    private List<QueryTarget> inferComparisonTargets(String query) {
        return queryRewritePolicy.parse(query).comparisonTargets();
    }

    private boolean containsSeoul(String query) {
        return query != null && (query.contains("서울") || query.contains("서울시") || query.contains("서울특별시"));
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(MAX_LIMIT, limit);
    }

    private boolean matchesTarget(DecisionCandidate candidate, QueryTarget target) {
        String dongName = candidate.dongName() == null ? "" : candidate.dongName();
        String apartmentName = candidate.apartmentName() == null ? "" : candidate.apartmentName();
        if (target.kind() == QueryTargetKind.REGION) {
            return dongName.contains(target.value()) || target.value().contains(dongName);
        }
        if (target.kind() == QueryTargetKind.APARTMENT) {
            return apartmentName.contains(target.value()) || target.value().contains(apartmentName);
        }
        return dongName.contains(target.value())
                || target.value().contains(dongName)
                || apartmentName.contains(target.value())
                || target.value().contains(apartmentName);
    }
}
