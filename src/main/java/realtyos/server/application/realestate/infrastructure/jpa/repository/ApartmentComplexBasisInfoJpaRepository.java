package realtyos.server.application.realestate.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexBasisInfoJpaEntity;

public interface ApartmentComplexBasisInfoJpaRepository extends JpaRepository<ApartmentComplexBasisInfoJpaEntity, Long> {

    boolean existsByKaptCode(String kaptCode);
}
