package realtyos.server.application.realestate.domain;

import java.util.List;

public interface ApartmentComplexRepository {

    List<ApartmentComplex> saveAll(List<ApartmentComplex> complexes);

    boolean existsByKaptCode(String kaptCode);
}
