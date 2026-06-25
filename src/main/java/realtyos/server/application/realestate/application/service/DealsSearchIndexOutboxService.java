package realtyos.server.application.realestate.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import realtyos.server.application.realestate.domain.Deals;
import realtyos.server.application.realestate.domain.DealsRepository;
import realtyos.server.application.realestate.domain.DealsSearchIndexQueue;
import realtyos.server.application.realestate.domain.DealsSearchIndexRequest;
import realtyos.server.application.realestate.domain.DealsSearchIndexer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DealsSearchIndexOutboxService {

    private final DealsSearchIndexQueue queue;
    private final DealsRepository dealsRepository;
    private final DealsSearchIndexer searchIndexer;

    public int processPending(int limit, int maxRetries) {
        List<DealsSearchIndexRequest> requests = queue.findPending(limit);
        if (requests.isEmpty()) {
            return 0;
        }

        List<Long> dealIds = requests.stream()
                .map(DealsSearchIndexRequest::dealId)
                .toList();
        Map<Long, Deals> dealsById = dealsRepository.findByIds(dealIds).stream()
                .filter(deal -> deal.id() != null)
                .collect(Collectors.toMap(Deals::id, Function.identity(), (a, b) -> a));

        List<Deals> dealsToIndex = requests.stream()
                .map(request -> dealsById.get(request.dealId()))
                .filter(deal -> deal != null)
                .toList();

        try {
            searchIndexer.indexAll(dealsToIndex);
            queue.markSucceeded(requests.stream()
                    .map(DealsSearchIndexRequest::id)
                    .toList());
            return dealsToIndex.size();
        } catch (Exception e) {
            for (DealsSearchIndexRequest request : requests) {
                int nextRetryCount = request.retryCount() + 1;
                queue.markFailed(request.id(), nextRetryCount, maxRetries, e.getMessage());
            }
            log.warn("실거래 Elasticsearch outbox 처리 실패 - 요청 {}건", requests.size(), e);
            return 0;
        }
    }
}
