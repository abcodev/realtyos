package realtyos.server.application.rag.application;

import org.springframework.stereotype.Component;
import realtyos.server.application.rag.domain.ParsedRealestateQuery;
import realtyos.server.application.rag.domain.QueryIntent;
import realtyos.server.application.rag.domain.RealestateQueryParser;

@Component
public class RagAnswerRouter {

    private final RealestateQueryParser queryParser = new RealestateQueryParser();

    public RagAnswerRoute route(String query) {
        ParsedRealestateQuery parsed = queryParser.parse(query);
        return new RagAnswerRoute(toRouteType(parsed.intent()), parsed.intentReason());
    }

    private RagAnswerRouteType toRouteType(QueryIntent intent) {
        return switch (intent) {
            case COMPARISON -> RagAnswerRouteType.COMPARISON;
            case RECOMMENDATION -> RagAnswerRouteType.RECOMMENDATION;
            case MARKET_PRICE -> RagAnswerRouteType.MARKET_PRICE;
            case SEARCH -> RagAnswerRouteType.SEARCH;
        };
    }
}
