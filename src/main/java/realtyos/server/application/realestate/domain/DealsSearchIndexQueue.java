package realtyos.server.application.realestate.domain;

import java.util.List;

public interface DealsSearchIndexQueue {

    void enqueueAll(List<Long> dealIds);

    List<DealsSearchIndexRequest> findPending(int limit);

    void markSucceeded(List<Long> ids);

    void markFailed(Long id, int nextRetryCount, int maxRetries, String errorMessage);
}
