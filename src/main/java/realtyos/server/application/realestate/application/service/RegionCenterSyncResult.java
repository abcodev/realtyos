package realtyos.server.application.realestate.application.service;

public record RegionCenterSyncResult(
        String level,
        int requestedCount,
        int syncedCount,
        int failedCount
) {
}
