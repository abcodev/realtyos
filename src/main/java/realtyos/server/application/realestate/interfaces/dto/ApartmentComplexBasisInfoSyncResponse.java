package realtyos.server.application.realestate.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import realtyos.server.application.realestate.application.service.ApartmentComplexBasisInfoSyncResult;

@Schema(description = "아파트 기본정보 수집 결과")
public record ApartmentComplexBasisInfoSyncResponse(
        int requestedLimit,
        int targetCount,
        int savedCount,
        int skippedCount,
        int failedCount
) {

    public static ApartmentComplexBasisInfoSyncResponse from(ApartmentComplexBasisInfoSyncResult result) {
        return new ApartmentComplexBasisInfoSyncResponse(
                result.requestedLimit(),
                result.targetCount(),
                result.savedCount(),
                result.skippedCount(),
                result.failedCount()
        );
    }
}
