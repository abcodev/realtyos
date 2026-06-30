package realtyos.server.application.realestate.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.realestate.domain.ApartmentComplexBasisInfo;
import realtyos.server.application.realestate.domain.ApartmentComplexBasisInfoRepository;
import realtyos.server.application.realestate.infrastructure.jpa.mapper.ApartmentComplexBasisInfoMapper;
import realtyos.server.application.realestate.infrastructure.jpa.repository.ApartmentComplexBasisInfoJpaRepository;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApartmentComplexBasisInfoRepositoryJpaAdaptor implements ApartmentComplexBasisInfoRepository {

    private final ApartmentComplexBasisInfoJpaRepository jpaRepository;
    private final ApartmentComplexBasisInfoMapper mapper;

    @Override
    @Transactional
    public ApartmentComplexBasisInfo save(ApartmentComplexBasisInfo basisInfo) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(basisInfo)));
    }

    @Override
    public boolean existsByKaptCode(String kaptCode) {
        return jpaRepository.existsByKaptCode(kaptCode);
    }
}
