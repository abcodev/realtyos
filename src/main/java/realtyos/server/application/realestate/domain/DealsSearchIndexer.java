package realtyos.server.application.realestate.domain;

import java.util.List;

public interface DealsSearchIndexer {

    void indexAll(List<Deals> deals);
}
