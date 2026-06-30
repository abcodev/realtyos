package realtyos.server.application.realestate.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import realtyos.server.application.common.response.ApiResponse;
import realtyos.server.application.realestate.application.service.ApartmentComplexService;
import realtyos.server.application.realestate.interfaces.dto.ApartmentComplexBasisInfoSyncResponse;
import realtyos.server.application.realestate.interfaces.dto.ApartmentComplexSyncResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/realestate/apartment-complexes")
@Tag(name = "Apartment Complex", description = "아파트 단지 정보 API")
public class ApartmentComplexController {

    private final ApartmentComplexService service;

    @PostMapping("/sync")
    @Operation(summary = "아파트 단지 정보 수집", description = "공공데이터 AptListService3 getTotalAptList3 응답을 저장합니다.")
    public ApiResponse<ApartmentComplexSyncResponse> sync(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "1000") int numOfRows
    ) {
        return ApiResponse.success(ApartmentComplexSyncResponse.from(
                service.fetchAndSaveTotalAptList(pageNo, numOfRows)
        ));
    }

    @PostMapping("/basis-info/sync")
    @Operation(summary = "아파트 기본정보 수집", description = "저장된 kaptCode로 AptBasisInfoServiceV4 getAphusBassInfoV4 응답을 저장합니다.")
    public ApiResponse<ApartmentComplexBasisInfoSyncResponse> syncBasisInfo(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.success(ApartmentComplexBasisInfoSyncResponse.from(
                service.fetchAndSaveBasisInfo(limit)
        ));
    }
}
