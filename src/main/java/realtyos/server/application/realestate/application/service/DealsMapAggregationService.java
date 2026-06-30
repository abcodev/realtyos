package realtyos.server.application.realestate.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import realtyos.server.application.realestate.domain.DealsMapAggregation;
import realtyos.server.application.realestate.domain.DealsMapAggregationCondition;
import realtyos.server.application.realestate.domain.DealsMapAggregationRepository;
import realtyos.server.application.realestate.domain.RegionResolution;
import realtyos.server.application.realestate.domain.RegionResolver;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DealsMapAggregationService {

    private final DealsMapAggregationRepository aggregationRepository;
    private final RegionResolver regionResolver;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final TypeReference<List<DealsMapAggregation>> AGGREGATION_LIST_TYPE = new TypeReference<>() {
    };

    public List<DealsMapAggregation> aggregate(DealsMapAggregationCondition condition) {
        String cacheKey = cacheKey(condition);
        List<DealsMapAggregation> cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }
        RegionResolution regionResolution = condition.region() == null || condition.region().isBlank()
                ? RegionResolution.empty(null)
                : regionResolver.resolve(condition.region());
        List<DealsMapAggregation> result = aggregationRepository.aggregate(condition, regionResolution);
        writeCache(cacheKey, result);
        return result;
    }

    private String cacheKey(DealsMapAggregationCondition condition) {
        return "realestate:deals:map:aggregates:"
                + safe(condition.region()) + ":"
                + condition.normalizedGroupLevel() + ":"
                + safe(condition.year()) + ":"
                + safe(condition.month()) + ":"
                + safe(condition.minPrice()) + ":"
                + safe(condition.maxPrice()) + ":"
                + safe(condition.minArea()) + ":"
                + safe(condition.maxArea()) + ":"
                + condition.normalizedLimit();
    }

    private List<DealsMapAggregation> readCache(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, AGGREGATION_LIST_TYPE);
        } catch (Exception e) {
            return null;
        }
    }

    private void writeCache(String key, List<DealsMapAggregation> result) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (JsonProcessingException ignored) {
        }
    }

    private String safe(Object value) {
        return value == null ? "_" : String.valueOf(value).trim();
    }

    public void evictMapCache() {
        try {
            var keys = redisTemplate.keys("realestate:deals:map:aggregates:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
    }
}
