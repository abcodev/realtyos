package realtyos.server.application.realestate.application.service;

import org.junit.jupiter.api.Test;
import realtyos.server.application.rag.domain.RagSearchCondition;
import realtyos.server.application.realestate.domain.DecisionCandidate;
import realtyos.server.application.realestate.domain.DecisionDealSample;
import realtyos.server.application.realestate.domain.DecisionResult;
import realtyos.server.application.realestate.domain.DecisionScoreBreakdown;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RealestateDecisionServiceTest {

    @Test
    void doesNotTreatSingleRegionCandidateRecommendationAsMultiTargetComparison() throws Exception {
        List<String> targets = inferComparisonTargets("마포에서 갈아타기 후보를 비교해줘");

        assertThat(targets).isEmpty();
    }

    @Test
    void extractsBothAdministrativeRegionsForComparisonQuestion() throws Exception {
        List<String> targets = inferComparisonTargets("대치동과 역삼동 시세 비교해줘");

        assertThat(targets).containsExactly("대치동", "역삼동");
    }

    @Test
    void extractsApartmentAndRegionForMixedComparisonQuestion() throws Exception {
        List<String> targets = inferComparisonTargets("잠실엘스랑 대치동 비교해줘");

        assertThat(targets).containsExactly("대치동", "잠실엘스");
    }

    @Test
    void decisionKeepsCandidatesSeparatedByComparisonTargets() {
        FakeDealAnalysisService dealAnalysisService = new FakeDealAnalysisService();
        RealestateDecisionService service = new RealestateDecisionService(
                dealAnalysisService,
                new DecisionScoreService(),
                new DecisionTargetSummaryBuilder(),
                new DecisionResultFormatter()
        );

        DecisionResult result = service.decide("대치동과 역삼동 시세 비교해줘", 10, null);

        assertThat(result.comparisonTargets()).containsExactly("대치동", "역삼동");
        assertThat(result.candidates())
                .extracting(DecisionCandidate::dongName)
                .contains("대치동", "역삼동");
        assertThat(dealAnalysisService.requestedRegions()).contains("대치동", "역삼동");
    }

    @Test
    void decisionFallsBackToApartmentSearchForApartmentComparisonTarget() {
        FakeDealAnalysisService dealAnalysisService = new FakeDealAnalysisService();
        RealestateDecisionService service = new RealestateDecisionService(
                dealAnalysisService,
                new DecisionScoreService(),
                new DecisionTargetSummaryBuilder(),
                new DecisionResultFormatter()
        );

        DecisionResult result = service.decide("잠실엘스랑 대치동 비교해줘", 10, null);

        assertThat(result.comparisonTargets()).containsExactly("대치동", "잠실엘스");
        assertThat(result.candidates())
                .extracting(DecisionCandidate::apartmentName)
                .contains("한보미도맨션1", "잠실엘스");
        assertThat(dealAnalysisService.requestedApartments()).contains("잠실엘스");
    }

    private List<String> inferComparisonTargets(String query) throws Exception {
        RealestateDecisionService service = new RealestateDecisionService(null, null, null, null);
        Method method = RealestateDecisionService.class.getDeclaredMethod("inferComparisonTargets", String.class);
        method.setAccessible(true);

        Object result = method.invoke(service, query);
        assertThat(result).isInstanceOf(List.class);
        List<?> targets = (List<?>) result;
        List<String> values = new ArrayList<>();
        for (Object target : targets) {
            values.add(extractTargetValue(target));
        }
        return values;
    }

    private String extractTargetValue(Object target) throws Exception {
        try {
            Method value = target.getClass().getDeclaredMethod("value");
            value.setAccessible(true);
            return (String) value.invoke(target);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private static class FakeDealAnalysisService extends DealAnalysisService {

        private final List<RagSearchCondition> requestedConditions = new ArrayList<>();

        private FakeDealAnalysisService() {
            super(null, null);
        }

        @Override
        public List<DecisionCandidate> findCandidates(RagSearchCondition condition, int limit) {
            requestedConditions.add(condition);
            if (condition == null) {
                return List.of();
            }
            if ("대치동".equals(condition.region())) {
                return List.of(candidate("한보미도맨션1", "11680", "대치동", 470000L, 84.0));
            }
            if ("역삼동".equals(condition.region())) {
                return List.of(candidate("역삼푸르지오", "11680", "역삼동", 298000L, 84.0));
            }
            if ("잠실엘스".equals(condition.apartmentName())) {
                return List.of(candidate("잠실엘스", "11710", "잠실동", 300000L, 84.0));
            }
            return List.of();
        }

        private List<String> requestedRegions() {
            return requestedConditions.stream()
                    .map(RagSearchCondition::region)
                    .filter(region -> region != null)
                    .toList();
        }

        private List<String> requestedApartments() {
            return requestedConditions.stream()
                    .map(RagSearchCondition::apartmentName)
                    .filter(apartmentName -> apartmentName != null)
                    .toList();
        }

        private DecisionCandidate candidate(String apartmentName, String regionCode, String dongName, Long price, Double area) {
            return new DecisionCandidate(
                    apartmentName,
                    regionCode,
                    dongName,
                    "2026-05-27",
                    price,
                    price,
                    price,
                    price,
                    area,
                    area,
                    area,
                    3L,
                    Math.round(price / (area / 3.305785)),
                    0,
                    new DecisionScoreBreakdown(0, 0, 0, 0),
                    List.of(),
                    List.of(),
                    List.of(new DecisionDealSample(
                            1L,
                            apartmentName,
                            regionCode,
                            dongName,
                            "2026-05-27",
                            price,
                            area,
                            "10"
                    ))
            );
        }
    }
}
