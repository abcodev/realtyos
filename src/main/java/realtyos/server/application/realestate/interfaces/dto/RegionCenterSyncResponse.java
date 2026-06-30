package realtyos.server.application.realestate.interfaces.dto;

import realtyos.server.application.realestate.application.service.RegionCenterSyncResult;

public record RegionCenterSyncResponse(
        String level,
        int requestedCount,
        int syncedCount,
        int failedCount
) {

    public static RegionCenterSyncResponse from(RegionCenterSyncResult result) {
        return new RegionCenterSyncResponse(
                result.level(),
                result.requestedCount(),
                result.syncedCount(),
                result.failedCount()
        );
    }
}
