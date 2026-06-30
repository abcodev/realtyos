package realtyos.server.application.realestate.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import realtyos.server.application.realestate.domain.DealsMapGroupLevel;

public record RegionCenterUpsertRequest(
        @NotNull DealsMapGroupLevel regionLevel,
        @NotBlank String regionKey,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
