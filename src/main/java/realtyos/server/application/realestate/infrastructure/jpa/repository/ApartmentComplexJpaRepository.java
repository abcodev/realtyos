package realtyos.server.application.realestate.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexJpaEntity;

public interface ApartmentComplexJpaRepository extends JpaRepository<ApartmentComplexJpaEntity, Long> {

    boolean existsByKaptCode(String kaptCode);
}
