package realtyos.server.application.realestate.domain;

import java.util.List;

public interface DealsMapAggregationRepository {

    List<DealsMapAggregation> aggregate(DealsMapAggregationCondition condition, RegionResolution regionResolution);
}
