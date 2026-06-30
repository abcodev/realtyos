package realtyos.server.application.realestate.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import realtyos.server.application.realestate.infrastructure.jpa.entity.ApartmentComplexJpaEntity;

import java.util.List;

public interface ApartmentComplexJpaRepository extends JpaRepository<ApartmentComplexJpaEntity, Long> {

    boolean existsByKaptCode(String kaptCode);

    @Query(value = """
            SELECT c.kapt_code
            FROM real_estate_apartment_complex c
            WHERE c.active = true
            AND NOT EXISTS (
                SELECT 1
                FROM real_estate_apartment_complex_basis_info b
                WHERE b.kapt_code = c.kapt_code
                  AND b.active = true
            )
            ORDER BY c.id
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findKaptCodesWithoutBasisInfo(int limit);

    @Query(value = """
            SELECT c.kapt_code
            FROM real_estate_apartment_complex c
            WHERE c.active = true
            ORDER BY c.id
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findActiveKaptCodes(int limit);
}
