package realtyos.server.application.realestate.application.service;

import lombok.Builder;

@Builder
public record ApartmentComplexBasisInfoSyncResult(
        int requestedLimit,
        int targetCount,
        int savedCount,
        int skippedCount,
        int failedCount
) {
}
