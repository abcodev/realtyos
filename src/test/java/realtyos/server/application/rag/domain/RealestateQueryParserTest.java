package realtyos.server.application.rag.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealestateQueryParserTest {

    private final RealestateQueryParser parser = new RealestateQueryParser();

    @Test
    void parsesPurchaseAvailabilityQuestion() {
        ParsedRealestateQuery parsed = parser.parse("서울에서 4억 미만으로 살 수있는 10평대 아파트 있어");

        assertThat(parsed.intent()).isEqualTo(QueryIntent.RECOMMENDATION);
        assertThat(parsed.intentReason()).isEqualTo("purchase_availability_intent");
        assertThat(parsed.condition().region()).isEqualTo("서울");
        assertThat(parsed.condition().maxPrice()).isEqualTo(40000L);
        assertThat(parsed.condition().minArea()).isEqualTo(33.06);
        assertThat(parsed.condition().maxArea()).isEqualTo(66.12);
    }

    @Test
    void parsesAdministrativeRegionComparisonQuestion() {
        ParsedRealestateQuery parsed = parser.parse("대치동과 역삼동 시세 비교해줘");

        assertThat(parsed.intent()).isEqualTo(QueryIntent.COMPARISON);
        assertThat(parsed.condition().region()).isNull();
        assertThat(parsed.comparisonTargets())
                .extracting(QueryTarget::value)
                .containsExactly("대치동", "역삼동");
    }

    @Test
    void parsesMixedApartmentAndRegionComparisonQuestion() {
        ParsedRealestateQuery parsed = parser.parse("잠실엘스랑 대치동 비교해줘");

        assertThat(parsed.intent()).isEqualTo(QueryIntent.COMPARISON);
        assertThat(parsed.comparisonTargets())
                .extracting(QueryTarget::value)
                .containsExactly("대치동", "잠실엘스");
    }

    @Test
    void keepsSingleRegionCandidateQuestionAsRecommendation() {
        ParsedRealestateQuery parsed = parser.parse("마포에서 갈아타기 후보를 비교해줘");

        assertThat(parsed.intent()).isEqualTo(QueryIntent.RECOMMENDATION);
        assertThat(parsed.condition().region()).isEqualTo("마포");
        assertThat(parsed.comparisonTargets()).isEmpty();
    }
}
