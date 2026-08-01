package realtyos.server.application.realestate.infrastructure.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import realtyos.server.application.realestate.domain.Deals;
import realtyos.server.application.realestate.domain.DealsSearchCondition;
import realtyos.server.application.realestate.domain.DealsSearchIndexer;
import realtyos.server.application.realestate.domain.DealsSearchQueryRepository;
import realtyos.server.application.realestate.domain.DealsSearchResult;
import realtyos.server.application.realestate.domain.RegionResolution;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DealsElasticsearchRepository implements DealsSearchIndexer, DealsSearchQueryRepository {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.elasticsearch.enabled:true}")
    private boolean enabled;

    @Value("${app.elasticsearch.base-url:http://localhost:19200}")
    private String baseUrl;

    @Value("${app.elasticsearch.deals-index:real_estate_deals}")
    private String indexName;

    @Value("${app.elasticsearch.bulk-batch-size:1000}")
    private int bulkBatchSize;

    @Override
    public void indexAll(List<Deals> deals) {
        if (!enabled || deals == null || deals.isEmpty()) {
            return;
        }
        try {
            ensureIndex();
            int batchSize = Math.max(100, Math.min(5_000, bulkBatchSize));
            for (int start = 0; start < deals.size(); start += batchSize) {
                bulkIndex(deals.subList(start, Math.min(start + batchSize, deals.size())));
            }
        } catch (Exception e) {
            log.warn("Elasticsearch 실거래 색인 실패 - count: {}", deals.size(), e);
            throw new IllegalStateException("Elasticsearch 실거래 색인에 실패했습니다.", e);
        }
    }

    private void bulkIndex(List<Deals> deals) throws Exception {
        StringBuilder bulk = new StringBuilder();
        for (Deals deal : deals) {
            DealsElasticsearchDocument document = DealsElasticsearchDocument.from(deal);
            bulk.append(objectMapper.writeValueAsString(Map.of("index", Map.of("_index", indexName, "_id", document.id()))))
                    .append('\n');
            bulk.append(objectMapper.writeValueAsString(document)).append('\n');
        }
        String responseBody = restClient.post()
                .uri(baseUrl + "/_bulk")
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(bulk.toString().getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .body(String.class);
        JsonNode response = objectMapper.readTree(responseBody);
        if (response != null && response.path("errors").asBoolean(false)) {
            throw new IllegalStateException("Elasticsearch bulk item failure: " + firstBulkError(response));
        }
    }

    private String firstBulkError(JsonNode response) {
        for (JsonNode item : response.path("items")) {
            JsonNode error = item.path("index").path("error");
            if (!error.isMissingNode()) {
                return error.toString();
            }
        }
        return response.toString();
    }

    @Override
    public List<DealsSearchResult> search(DealsSearchCondition condition, RegionResolution regionResolution) {
        if (!enabled) {
            return List.of();
        }
        try {
            Map<String, Object> body = Map.of(
                    "size", condition.normalizedLimit(),
                    "query", Map.of("bool", Map.of("filter", filters(condition, regionResolution))),
                    "sort", List.of(Map.of("dealDate", Map.of("order", "desc", "missing", "_last")))
            );
            String responseBody = restClient.post()
                    .uri(baseUrl + "/" + indexName + "/_search")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            if (responseBody == null || responseBody.isBlank()) {
                return List.of();
            }
            JsonNode response = objectMapper.readTree(responseBody);
            List<DealsSearchResult> results = new ArrayList<>();
            for (JsonNode hit : response.path("hits").path("hits")) {
                JsonNode source = hit.path("_source");
                DealsElasticsearchDocument document = objectMapper.treeToValue(source, DealsElasticsearchDocument.class);
                results.add(document.toSearchResult());
            }
            return results;
        } catch (RestClientResponseException e) {
            log.warn("Elasticsearch 실거래 검색 실패 - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.warn("Elasticsearch 실거래 검색 실패", e);
            return List.of();
        }
    }

    private List<Map<String, Object>> filters(DealsSearchCondition condition, RegionResolution regionResolution) {
        List<Map<String, Object>> filters = new ArrayList<>();
        if (regionResolution != null && regionResolution.hasFilter()) {
            switch (regionResolution.type()) {
                case SGG -> filters.add(Map.of("terms", Map.of("sggCode", regionResolution.sggCodes())));
                case DONG -> filters.add(Map.of("match", Map.of("umdName", regionResolution.dongName())));
                case KEYWORD -> filters.add(Map.of("multi_match", Map.of(
                        "query", regionResolution.keyword(),
                        "fields", List.of("umdName", "aptName", "aptName.ngram")
                )));
                case NONE -> {
                }
            }
        }
        if (hasText(condition.apartmentName())) {
            filters.add(apartmentNameQuery(condition.apartmentName()));
        }
        if (condition.year() != null) {
            filters.add(Map.of("term", Map.of("dealYear", condition.year())));
        }
        if (condition.month() != null) {
            filters.add(Map.of("term", Map.of("dealMonth", condition.month())));
        }
        addRange(filters, "dealAmountValue", condition.minPrice(), condition.maxPrice());
        addRange(filters, "exclusiveUseAreaValue", condition.minArea(), condition.maxArea());
        return filters;
    }

    private Map<String, Object> apartmentNameQuery(String apartmentName) {
        return Map.of("multi_match", Map.of(
                "query", apartmentName,
                "fields", List.of("aptName^2", "aptName.ngram")
        ));
    }

    private void addRange(List<Map<String, Object>> filters, String field, Number min, Number max) {
        if (min == null && max == null) {
            return;
        }
        Map<String, Object> range = new LinkedHashMap<>();
        if (min != null) {
            range.put("gte", min);
        }
        if (max != null) {
            range.put("lte", max);
        }
        filters.add(Map.of("range", Map.of(field, range)));
    }

    private void ensureIndex() {
        try {
            restClient.put()
                    .uri(baseUrl + "/" + indexName)
                    .body(indexMapping())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (!e.getResponseBodyAsString().contains("resource_already_exists_exception")) {
                throw e;
            }
        }
    }

    private Map<String, Object> indexMapping() {
        Map<String, Object> settings = Map.of(
                "number_of_shards", 1,
                "number_of_replicas", 0,
                "refresh_interval", "30s",
                "index.max_ngram_diff", 28,
                "analysis", Map.of(
                        "tokenizer", Map.of(
                                "apt_name_nori_tokenizer", Map.of(
                                        "type", "nori_tokenizer",
                                        "decompound_mode", "mixed",
                                        "discard_punctuation", true
                                ),
                                "apt_name_ngram_tokenizer", Map.of(
                                        "type", "ngram",
                                        "min_gram", 2,
                                        "max_gram", 30,
                                        "token_chars", List.of("letter", "digit")
                                )
                        ),
                        "analyzer", Map.of(
                                "apt_name_nori_analyzer", Map.of(
                                        "type", "custom",
                                        "tokenizer", "apt_name_nori_tokenizer",
                                        "filter", List.of("nori_readingform", "lowercase")
                                ),
                                "apt_name_ngram_analyzer", Map.of(
                                        "type", "custom",
                                        "tokenizer", "apt_name_ngram_tokenizer",
                                        "filter", List.of("lowercase")
                                )
                        )
                )
        );
        Map<String, Object> properties = Map.ofEntries(
                Map.entry("id", Map.of("type", "keyword")),
                Map.entry("sggCode", Map.of("type", "keyword")),
                Map.entry("umdName", Map.of("type", "text", "analyzer", "standard", "fields", Map.of("keyword", Map.of("type", "keyword")))),
                Map.entry("aptName", Map.of(
                        "type", "text",
                        "analyzer", "apt_name_nori_analyzer",
                        "search_analyzer", "apt_name_nori_analyzer",
                        "fields", Map.of(
                                "keyword", Map.of("type", "keyword"),
                                "ngram", Map.of(
                                        "type", "text",
                                        "analyzer", "apt_name_ngram_analyzer",
                                        "search_analyzer", "apt_name_nori_analyzer"
                                )
                        )
                )),
                Map.entry("jibun", Map.of("type", "keyword")),
                Map.entry("dealYear", Map.of("type", "integer")),
                Map.entry("dealMonth", Map.of("type", "integer")),
                Map.entry("dealDay", Map.of("type", "integer")),
                Map.entry("dealDate", Map.of("type", "date")),
                Map.entry("dealAmount", Map.of("type", "keyword")),
                Map.entry("dealAmountValue", Map.of("type", "long")),
                Map.entry("exclusiveUseArea", Map.of("type", "keyword")),
                Map.entry("exclusiveUseAreaValue", Map.of("type", "double")),
                Map.entry("floor", Map.of("type", "keyword")),
                Map.entry("buildYear", Map.of("type", "keyword"))
        );
        return Map.of(
                "settings", settings,
                "mappings", Map.of("properties", properties)
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
