package realtyos.server.application.realestate.application.service;

import lombok.Builder;

@Builder
public record ApartmentComplexSyncResult(
        int requestedPageNo,
        int requestedNumOfRows,
        int totalCount,
        int fetchedCount,
        int upsertedCount,
        int deactivatedCount
) {
}
