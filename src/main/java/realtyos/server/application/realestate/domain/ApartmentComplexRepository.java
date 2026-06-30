package realtyos.server.application.realestate.domain;

import java.util.List;

public interface ApartmentComplexRepository {

    int upsertAll(List<ApartmentComplex> complexes);

    boolean existsByKaptCode(String kaptCode);

    List<String> findKaptCodesWithoutBasisInfo(int limit);

    List<String> findActiveKaptCodes(int limit);

    int markInactiveIfNotSynced();
}
