package realtyos.server.application.realestate.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.realestate.domain.ApartmentComplex;
import realtyos.server.application.realestate.domain.ApartmentComplexRepository;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexJpaEntity;
import realtyos.server.application.realestate.infrastructure.jpa.mapper.ApartmentComplexMapper;
import realtyos.server.application.realestate.infrastructure.jpa.repository.ApartmentComplexJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApartmentComplexRepositoryJpaAdaptor implements ApartmentComplexRepository {

    private final ApartmentComplexJpaRepository jpaRepository;
    private final ApartmentComplexMapper mapper;

    @Override
    @Transactional
    public List<ApartmentComplex> saveAll(List<ApartmentComplex> complexes) {
        List<ApartmentComplexJpaEntity> entities = complexes.stream()
                .map(mapper::toEntity)
                .toList();
        return jpaRepository.saveAll(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByKaptCode(String kaptCode) {
        return jpaRepository.existsByKaptCode(kaptCode);
    }
}
