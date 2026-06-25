package realtyos.server.application.realestate.domain;

import java.util.List;

public interface DealsRepository {

    List<Deals> saveAll(List<Deals> deals);

    List<Deals> findSearchIndexBatch(Integer year, Long afterId, int batchSize);

    List<Deals> findByIds(List<Long> ids);
}
