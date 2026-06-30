package realtyos.server.application.realestate.domain;

public interface ApartmentComplexBasisInfoRepository {

    ApartmentComplexBasisInfo save(ApartmentComplexBasisInfo basisInfo);

    int upsert(ApartmentComplexBasisInfo basisInfo);

    boolean existsByKaptCode(String kaptCode);

    int markInactiveForInactiveComplexes();
}
