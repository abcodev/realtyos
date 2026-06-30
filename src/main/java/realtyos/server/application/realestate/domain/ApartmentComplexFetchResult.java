package realtyos.server.application.realestate.domain;

import lombok.Builder;

@Builder
public record ApartmentComplexFetchResult(
        int pageNo,
        int numOfRows,
        int totalCount,
        int fetchedCount
) {
}
