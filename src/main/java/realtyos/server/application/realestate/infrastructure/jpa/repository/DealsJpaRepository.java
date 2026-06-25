package realtyos.server.application.realestate.infrastructure.jpa.repository;

import realtyos.server.application.realestate.infrastructure.jpa.entity.DealsJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DealsJpaRepository extends JpaRepository<DealsJpaEntity, Long> {

        List<DealsJpaEntity> findBySggCodeAndDealYearAndDealMonth(String sggCode, Integer dealYear, Integer dealMonth);

        @Query("""
                SELECT d
                FROM DealsJpaEntity d
                WHERE d.id > :afterId
                AND (:year IS NULL OR d.dealYear = :year)
                ORDER BY d.id ASC
                """)
        List<DealsJpaEntity> findSearchIndexBatch(
                @Param("year") Integer year,
                @Param("afterId") Long afterId,
                Pageable pageable
        );
}
