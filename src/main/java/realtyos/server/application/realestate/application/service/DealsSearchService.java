package realtyos.server.application.realestate.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import realtyos.server.application.realestate.domain.Deals;
import realtyos.server.application.realestate.domain.DealsSearchCondition;
import realtyos.server.application.realestate.domain.DealsSearchQueryRepository;
import realtyos.server.application.realestate.domain.DealsSearchResult;
import realtyos.server.application.realestate.domain.RegionResolution;
import realtyos.server.application.realestate.domain.RegionResolver;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DealsSearchService {

    private final DealsSearchQueryRepository searchRepository;
    private final RegionResolver regionResolver;

    public List<DealsSearchResult> search(DealsSearchCondition condition) {
        RegionResolution regionResolution = condition.region() == null || condition.region().isBlank()
                ? RegionResolution.empty(null)
                : regionResolver.resolve(condition.region());
        return searchRepository.search(condition, regionResolution);
    }
}
