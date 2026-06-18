package realtyos.server.application.realestate.domain;

public record DealsSearchIndexRequest(
        Long id,
        Long dealId,
        int retryCount
) {
}
