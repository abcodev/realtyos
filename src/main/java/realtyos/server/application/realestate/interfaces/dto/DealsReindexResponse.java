package realtyos.server.application.realestate.interfaces.dto;

import realtyos.server.application.realestate.application.service.DealsSearchReindexResult;

public record DealsReindexResponse(
        Integer year,
        int indexedCount,
        Long lastId,
        boolean completed
) {

    public static DealsReindexResponse from(DealsSearchReindexResult result) {
        return new DealsReindexResponse(
                result.year(),
                result.indexedCount(),
                result.lastId(),
                result.completed()
        );
    }
}
