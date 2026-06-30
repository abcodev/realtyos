package realtyos.server.application.realestate.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import realtyos.server.application.realestate.application.service.ApartmentComplexSyncResult;

@Schema(description = "아파트 단지 정보 수집 결과")
public record ApartmentComplexSyncResponse(
        int requestedPageNo,
        int requestedNumOfRows,
        int totalCount,
        int fetchedCount,
        int savedCount
) {

    public static ApartmentComplexSyncResponse from(ApartmentComplexSyncResult result) {
        return new ApartmentComplexSyncResponse(
                result.requestedPageNo(),
                result.requestedNumOfRows(),
                result.totalCount(),
                result.fetchedCount(),
                result.savedCount()
        );
    }
}
