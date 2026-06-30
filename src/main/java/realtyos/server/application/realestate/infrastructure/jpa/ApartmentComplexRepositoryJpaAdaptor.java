package realtyos.server.application.realestate.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import realtyos.server.application.realestate.domain.ApartmentComplex;
import realtyos.server.application.realestate.domain.ApartmentComplexRepository;
import realtyos.server.application.realestate.infrastructure.jpa.repository.ApartmentComplexJpaRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApartmentComplexRepositoryJpaAdaptor implements ApartmentComplexRepository {

    private final ApartmentComplexJpaRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int upsertAll(List<ApartmentComplex> complexes) {
        if (complexes.isEmpty()) {
            return 0;
        }

        String sql = """
                INSERT INTO real_estate_apartment_complex (
                    kapt_code, kapt_name, as1, as2, as3, as4, bjd_code, full_address,
                    active, last_synced_at, deleted_at, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (kapt_code) DO UPDATE
                SET kapt_name = EXCLUDED.kapt_name,
                    as1 = EXCLUDED.as1,
                    as2 = EXCLUDED.as2,
                    as3 = EXCLUDED.as3,
                    as4 = EXCLUDED.as4,
                    bjd_code = EXCLUDED.bjd_code,
                    full_address = EXCLUDED.full_address,
                    active = true,
                    last_synced_at = CURRENT_TIMESTAMP,
                    deleted_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                """;

        int[] counts = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ApartmentComplex complex = complexes.get(i);
                ps.setString(1, complex.kaptCode());
                ps.setString(2, complex.kaptName());
                ps.setString(3, complex.as1());
                ps.setString(4, complex.as2());
                ps.setString(5, complex.as3());
                ps.setString(6, complex.as4());
                ps.setString(7, complex.bjdCode());
                ps.setString(8, complex.fullAddress());
            }

            @Override
            public int getBatchSize() {
                return complexes.size();
            }
        });

        return counts.length;
    }

    @Override
    public boolean existsByKaptCode(String kaptCode) {
        return jpaRepository.existsByKaptCode(kaptCode);
    }

    @Override
    public List<String> findKaptCodesWithoutBasisInfo(int limit) {
        return jpaRepository.findKaptCodesWithoutBasisInfo(limit);
    }

    @Override
    public List<String> findActiveKaptCodes(int limit) {
        return jpaRepository.findActiveKaptCodes(limit);
    }

    @Override
    @Transactional
    public int markInactiveIfNotSynced() {
        return jdbcTemplate.update("""
                UPDATE real_estate_apartment_complex
                SET active = false,
                    deleted_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE active = true
                  AND (last_synced_at IS NULL OR last_synced_at < CURRENT_DATE)
                """);
    }
}
