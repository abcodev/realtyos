package realtyos.server.application.realestate.domain;

import java.util.List;

public interface DealsSearchQueryRepository {

    List<DealsSearchResult> search(DealsSearchCondition condition, RegionResolution regionResolution);
}
