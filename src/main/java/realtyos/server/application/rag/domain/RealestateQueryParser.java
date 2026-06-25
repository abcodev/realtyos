package realtyos.server.application.rag.domain;

import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RealestateQueryParser {

    private static final Pattern EOK_PRICE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*억\\s*(이상|초과|부터|이하|미만|까지|대)?");
    private static final Pattern MANWON_PRICE_PATTERN = Pattern.compile("(\\d{4,})\\s*만\\s*원?\\s*(이상|초과|부터|이하|미만|까지)?");
    private static final Pattern AREA_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:제곱|평방|㎡|m2|m\\^2)\\s*(이상|초과|부터|이하|미만|까지)?");
    private static final Pattern PYEONG_RANGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*평\\s*대");
    private static final Pattern PYEONG_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*평(?:형)?");
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("(20\\d{2})\\s*년(?:\\s*(\\d{1,2})\\s*월)?");
    private static final Pattern ADMIN_REGION_PATTERN = Pattern.compile("([가-힣]{2,}(?:구|동|읍|면|리))");
    private static final Pattern LEADING_REGION_PATTERN = Pattern.compile("^([가-힣]{2,10})(?:\\s|$)");
    private static final Pattern REGION_PARTICLE_PATTERN = Pattern.compile("(에서|으로|로|의)$");
    private static final Pattern COMPARISON_SPLIT_PATTERN = Pattern.compile("\\s*(?:이랑|랑|하고|와|과|vs|VS|비교)\\s*");
    private static final double SQUARE_METERS_PER_PYEONG = 3.305785;

    public ParsedRealestateQuery parse(String query) {
        String text = query == null ? "" : query;
        String normalized = text.replace(",", "");
        List<QueryTarget> comparisonTargets = inferComparisonTargets(normalized);
        List<String> comparisonRegions = inferComparisonRegions(normalized);
        QueryIntent intent = inferIntent(normalized, comparisonTargets, comparisonRegions);
        String intentReason = inferIntentReason(normalized, intent);

        return new ParsedRealestateQuery(
                query,
                intent,
                intentReason,
                inferCondition(normalized, intent),
                comparisonTargets,
                comparisonRegions
        );
    }

    private QueryIntent inferIntent(String text, List<QueryTarget> comparisonTargets, List<String> comparisonRegions) {
        if (text == null || text.isBlank()) {
            return QueryIntent.SEARCH;
        }
        if (comparisonTargets.size() > 1 || comparisonRegions.size() > 1) {
            return QueryIntent.COMPARISON;
        }
        if (isPurchaseAvailabilityQuery(text)
                || containsAny(text, "추천", "후보", "의사결정", "살만", "매수", "투자", "실거주", "골라", "괜찮", "나아", "갈아타기")) {
            return QueryIntent.RECOMMENDATION;
        }
        if (containsAny(text, "시세", "흐름", "어때", "어떤가", "최근 거래 흐름", "평균가", "중위가", "평당가")) {
            return QueryIntent.MARKET_PRICE;
        }
        return QueryIntent.SEARCH;
    }

    private String inferIntentReason(String text, QueryIntent intent) {
        if (text == null || text.isBlank()) {
            return "empty_query";
        }
        return switch (intent) {
            case COMPARISON -> "comparison_intent";
            case RECOMMENDATION -> isPurchaseAvailabilityQuery(text) ? "purchase_availability_intent" : "recommendation_intent";
            case MARKET_PRICE -> "market_price_intent";
            case SEARCH -> "rag_search_intent";
        };
    }

    private RagSearchCondition inferCondition(String text, QueryIntent intent) {
        String region = inferRegion(text, intent);
        Boolean recentFirst = containsAny(text, "최근", "최신", "요즘", "근래", "최근순") ? Boolean.TRUE : null;
        PriceRange priceRange = inferPriceRange(text);
        AreaRange areaRange = inferAreaRange(text);
        YearMonthRange yearMonthRange = inferYearMonthRange(text);

        return new RagSearchCondition(
                region,
                null,
                yearMonthRange.fromYear(),
                yearMonthRange.fromMonth(),
                yearMonthRange.toYear(),
                yearMonthRange.toMonth(),
                priceRange.minPrice(),
                priceRange.maxPrice(),
                areaRange.minArea(),
                areaRange.maxArea(),
                recentFirst
        );
    }

    public List<String> inferComparisonRegions(String query) {
        String text = query == null ? "" : query;
        if (!isComparisonQuery(text)) {
            return List.of();
        }
        LinkedHashMap<String, String> regions = new LinkedHashMap<>();
        Matcher matcher = ADMIN_REGION_PATTERN.matcher(text);
        while (matcher.find()) {
            String region = matcher.group(1);
            regions.putIfAbsent(region, region);
        }
        return regions.size() > 1 ? List.copyOf(regions.values()) : List.of();
    }

    public List<QueryTarget> inferComparisonTargets(String query) {
        if (query == null || !isComparisonQuery(query)) {
            return List.of();
        }
        List<QueryTarget> regionTargets = inferAdministrativeRegionTargets(query);
        if (regionTargets.size() > 1) {
            return regionTargets;
        }
        if (isSingleRegionCandidateComparison(query)) {
            return List.of();
        }

        String normalized = query
                .replace("시세", " ")
                .replace("어때", " ")
                .replace("어떤가", " ")
                .replace("좋을까", " ")
                .replace("좋아", " ")
                .replace("해줘", " ")
                .replace("해달라", " ")
                .replace("해달라고", " ")
                .replace("비교해줘", " ")
                .replace("비교해달라", " ")
                .replace("갈아타기", " ")
                .replace("후보를", " ")
                .replace("후보", " ")
                .replace("추천", " ")
                .replace("더", " ")
                .trim();
        List<QueryTarget> targets = new ArrayList<>(regionTargets);
        for (String token : COMPARISON_SPLIT_PATTERN.split(normalized)) {
            for (String part : token.split("\\s+")) {
                String value = part.replaceAll("[^가-힣A-Za-z0-9()\\-]", "").trim();
                if (value.isBlank() || value.length() < 2 || value.equals("중") || value.equals("어디")
                        || value.equals("비교") || value.equals("해줘") || value.equals("해달라")) {
                    continue;
                }
                if (targets.stream().anyMatch(target -> target.value().equals(value))) {
                    continue;
                }
                targets.add(QueryTarget.from(value));
            }
        }
        return targets.size() > 1 ? targets : List.of();
    }

    private List<QueryTarget> inferAdministrativeRegionTargets(String query) {
        LinkedHashMap<String, QueryTarget> targets = new LinkedHashMap<>();
        Matcher matcher = ADMIN_REGION_PATTERN.matcher(query);
        while (matcher.find()) {
            String region = matcher.group(1);
            targets.putIfAbsent(region, new QueryTarget(region, QueryTargetKind.REGION));
        }
        return new ArrayList<>(targets.values());
    }

    private boolean isSingleRegionCandidateComparison(String query) {
        boolean candidateIntent = containsAny(query, "후보", "추천", "갈아타기");
        boolean explicitPairConnector = containsAny(query, " vs ", "VS", "와", "과", "랑", "하고", "이랑");
        return candidateIntent && !explicitPairConnector;
    }

    private String inferRegion(String text, QueryIntent intent) {
        LinkedHashMap<String, String> matchedRegions = new LinkedHashMap<>();
        findAdministrativeRegions(text).forEach(region -> matchedRegions.putIfAbsent(region, region));
        if (!matchedRegions.isEmpty() && intent == QueryIntent.COMPARISON) {
            return null;
        }
        if (!matchedRegions.isEmpty()) {
            return matchedRegions.values().stream().findFirst().orElse(null);
        }
        return inferLeadingRegion(text);
    }

    private List<String> findAdministrativeRegions(String text) {
        List<String> regions = new ArrayList<>();
        Matcher matcher = ADMIN_REGION_PATTERN.matcher(text);
        while (matcher.find()) {
            regions.add(matcher.group(1));
        }
        return regions;
    }

    private String inferLeadingRegion(String text) {
        if (!containsAny(text, "시세", "최근", "거래", "흐름", "아파트", "어때", "어떤가", "후보", "추천", "갈아타기", "비교", "살 수", "살수")) {
            return null;
        }
        Matcher matcher = LEADING_REGION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String candidate = REGION_PARTICLE_PATTERN.matcher(matcher.group(1)).replaceFirst("");
        if (candidate.length() < 2 || containsAny(candidate, "최근", "거래", "아파트", "시세", "후보", "추천", "비교")) {
            return null;
        }
        return candidate;
    }

    private boolean isComparisonQuery(String text) {
        return containsAny(text, "비교", "차이", "대비", " vs ", "VS", "와", "과", "랑", "하고", "이랑", "중 어디", "어디가");
    }

    private boolean isPurchaseAvailabilityQuery(String text) {
        boolean purchaseIntent = containsAny(text, "살 수", "살수", "구입 가능", "매수 가능", "살만한", "살만");
        boolean hasCondition = containsAny(text, "억", "만원", "미만", "이하", "이상", "평대", "평형", "㎡", "아파트");
        return purchaseIntent && hasCondition;
    }

    private PriceRange inferPriceRange(String text) {
        Matcher eokMatcher = EOK_PRICE_PATTERN.matcher(text);
        if (eokMatcher.find()) {
            long price = Math.round(Double.parseDouble(eokMatcher.group(1)) * 10000);
            return toPriceRange(price, eokMatcher.group(2), true);
        }

        Matcher manwonMatcher = MANWON_PRICE_PATTERN.matcher(text);
        if (manwonMatcher.find()) {
            long price = Long.parseLong(manwonMatcher.group(1));
            return toPriceRange(price, manwonMatcher.group(2), false);
        }

        return new PriceRange(null, null);
    }

    private PriceRange toPriceRange(long price, String qualifier, boolean eokExpression) {
        if (qualifier == null || qualifier.isBlank()) {
            if (eokExpression) {
                return new PriceRange(price, price + 9999);
            }
            return new PriceRange(price, null);
        }
        return switch (qualifier) {
            case "이상", "초과", "부터" -> new PriceRange(price, null);
            case "이하", "미만", "까지" -> new PriceRange(null, price);
            case "대" -> new PriceRange(price, price + 9999);
            default -> new PriceRange(null, null);
        };
    }

    private AreaRange inferAreaRange(String text) {
        Matcher pyeongRangeMatcher = PYEONG_RANGE_PATTERN.matcher(text);
        if (pyeongRangeMatcher.find()) {
            double pyeong = Double.parseDouble(pyeongRangeMatcher.group(1));
            return new AreaRange(toSquareMeters(pyeong), toSquareMeters(pyeong + 10));
        }

        Matcher pyeongMatcher = PYEONG_PATTERN.matcher(text);
        if (pyeongMatcher.find()) {
            double area = toSquareMeters(Double.parseDouble(pyeongMatcher.group(1)));
            return new AreaRange(Math.max(0, area - 6), area + 6);
        }

        Matcher matcher = AREA_PATTERN.matcher(text);
        if (!matcher.find()) {
            return new AreaRange(null, null);
        }
        double area = Double.parseDouble(matcher.group(1));
        String qualifier = matcher.group(2);
        if (qualifier != null && !qualifier.isBlank()) {
            return switch (qualifier) {
                case "이상", "초과", "부터" -> new AreaRange(area, null);
                case "이하", "미만", "까지" -> new AreaRange(null, area);
                default -> new AreaRange(Math.max(0, area - 5), area + 5);
            };
        }
        return new AreaRange(Math.max(0, area - 5), area + 5);
    }

    private double toSquareMeters(double pyeong) {
        return Math.round(pyeong * SQUARE_METERS_PER_PYEONG * 100.0) / 100.0;
    }

    private YearMonthRange inferYearMonthRange(String text) {
        if (text.contains("올해")) {
            int currentYear = Year.now().getValue();
            return new YearMonthRange(currentYear, 1, currentYear, 12);
        }
        if (text.contains("작년")) {
            int lastYear = Year.now().getValue() - 1;
            return new YearMonthRange(lastYear, 1, lastYear, 12);
        }

        Matcher matcher = YEAR_MONTH_PATTERN.matcher(text);
        if (!matcher.find()) {
            return new YearMonthRange(null, null, null, null);
        }

        int year = Integer.parseInt(matcher.group(1));
        String monthGroup = matcher.group(2);
        if (monthGroup == null) {
            return new YearMonthRange(year, 1, year, 12);
        }

        int month = Integer.parseInt(monthGroup);
        return new YearMonthRange(year, month, year, month);
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private record PriceRange(Long minPrice, Long maxPrice) {
    }

    private record AreaRange(Double minArea, Double maxArea) {
    }

    private record YearMonthRange(Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth) {
    }
}
