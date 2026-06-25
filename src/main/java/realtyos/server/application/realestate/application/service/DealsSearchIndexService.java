package realtyos.server.application.realestate.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import realtyos.server.application.realestate.domain.Deals;
import realtyos.server.application.realestate.domain.DealsRepository;
import realtyos.server.application.realestate.domain.DealsSearchIndexer;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DealsSearchIndexService {

    private final DealsRepository dealsRepository;
    private final DealsSearchIndexer searchIndexer;

    public DealsSearchReindexResult reindex(Integer year, Long afterId, Integer batchSize, Integer maxBatches) {
        int normalizedBatchSize = normalizedBatchSize(batchSize);
        int normalizedMaxBatches = normalizedMaxBatches(maxBatches);
        long lastId = afterId == null ? 0L : afterId;
        int indexedCount = 0;
        boolean completed = false;

        for (int batchNo = 0; batchNo < normalizedMaxBatches; batchNo++) {
            List<Deals> deals = dealsRepository.findSearchIndexBatch(year, lastId, normalizedBatchSize);
            if (deals.isEmpty()) {
                completed = true;
                break;
            }
            searchIndexer.indexAll(deals);
            indexedCount += deals.size();
            lastId = deals.getLast().id();
            if (deals.size() < normalizedBatchSize) {
                completed = true;
                break;
            }
        }

        return new DealsSearchReindexResult(year, indexedCount, lastId, completed);
    }

    private int normalizedBatchSize(Integer batchSize) {
        if (batchSize == null) {
            return 1000;
        }
        return Math.max(100, Math.min(10_000, batchSize));
    }

    private int normalizedMaxBatches(Integer maxBatches) {
        if (maxBatches == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, Math.min(10_000, maxBatches));
    }
}
