package realtyos.server.application.realestate.application.service;

public record DealsSearchReindexResult(
        Integer year,
        int indexedCount,
        Long lastId,
        boolean completed
) {
}
