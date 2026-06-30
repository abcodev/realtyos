package realtyos.server.application.realestate.domain;

import lombok.Builder;

@Builder
public record ApartmentComplex(
        Long id,
        String kaptCode,
        String kaptName,
        String as1,
        String as2,
        String as3,
        String as4,
        String bjdCode,
        String fullAddress
) {
}
