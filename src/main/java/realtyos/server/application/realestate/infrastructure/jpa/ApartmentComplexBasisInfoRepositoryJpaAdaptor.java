package realtyos.server.application.realestate.infrastructure.jpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public ApartmentComplexBasisInfo save(ApartmentComplexBasisInfo basisInfo) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(basisInfo)));
    }

    @Override
    @Transactional
    public int upsert(ApartmentComplexBasisInfo basisInfo) {
        return jdbcTemplate.update("""
                INSERT INTO real_estate_apartment_complex_basis_info (
                    zipcode, kapt_code, kapt_name, kapt_addr, code_sale_nm, code_heat_nm,
                    kapt_tarea, kapt_dong_cnt, kaptda_cnt, kapt_bcompany, kapt_acompany,
                    kapt_tel, kapt_fax, kapt_url, code_apt_nm, doro_juso, ho_cnt,
                    code_mgr_nm, code_hall_nm, kapt_usedate, kapt_marea, kapt_mparea60,
                    kapt_mparea85, kapt_mparea135, kapt_mparea136, priv_area, bjd_code,
                    kapt_top_floor, ktown_flr_no, kapt_base_floor, kaptd_ecntp,
                    active, last_synced_at, deleted_at, created_at, updated_at
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    true, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                ON CONFLICT (kapt_code) DO UPDATE
                SET zipcode = EXCLUDED.zipcode,
                    kapt_name = EXCLUDED.kapt_name,
                    kapt_addr = EXCLUDED.kapt_addr,
                    code_sale_nm = EXCLUDED.code_sale_nm,
                    code_heat_nm = EXCLUDED.code_heat_nm,
                    kapt_tarea = EXCLUDED.kapt_tarea,
                    kapt_dong_cnt = EXCLUDED.kapt_dong_cnt,
                    kaptda_cnt = EXCLUDED.kaptda_cnt,
                    kapt_bcompany = EXCLUDED.kapt_bcompany,
                    kapt_acompany = EXCLUDED.kapt_acompany,
                    kapt_tel = EXCLUDED.kapt_tel,
                    kapt_fax = EXCLUDED.kapt_fax,
                    kapt_url = EXCLUDED.kapt_url,
                    code_apt_nm = EXCLUDED.code_apt_nm,
                    doro_juso = EXCLUDED.doro_juso,
                    ho_cnt = EXCLUDED.ho_cnt,
                    code_mgr_nm = EXCLUDED.code_mgr_nm,
                    code_hall_nm = EXCLUDED.code_hall_nm,
                    kapt_usedate = EXCLUDED.kapt_usedate,
                    kapt_marea = EXCLUDED.kapt_marea,
                    kapt_mparea60 = EXCLUDED.kapt_mparea60,
                    kapt_mparea85 = EXCLUDED.kapt_mparea85,
                    kapt_mparea135 = EXCLUDED.kapt_mparea135,
                    kapt_mparea136 = EXCLUDED.kapt_mparea136,
                    priv_area = EXCLUDED.priv_area,
                    bjd_code = EXCLUDED.bjd_code,
                    kapt_top_floor = EXCLUDED.kapt_top_floor,
                    ktown_flr_no = EXCLUDED.ktown_flr_no,
                    kapt_base_floor = EXCLUDED.kapt_base_floor,
                    kaptd_ecntp = EXCLUDED.kaptd_ecntp,
                    active = true,
                    last_synced_at = CURRENT_TIMESTAMP,
                    deleted_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                """,
                basisInfo.zipcode(),
                basisInfo.kaptCode(),
                basisInfo.kaptName(),
                basisInfo.kaptAddr(),
                basisInfo.codeSaleNm(),
                basisInfo.codeHeatNm(),
                basisInfo.kaptTarea(),
                basisInfo.kaptDongCnt(),
                basisInfo.kaptdaCnt(),
                basisInfo.kaptBcompany(),
                basisInfo.kaptAcompany(),
                basisInfo.kaptTel(),
                basisInfo.kaptFax(),
                basisInfo.kaptUrl(),
                basisInfo.codeAptNm(),
                basisInfo.doroJuso(),
                basisInfo.hoCnt(),
                basisInfo.codeMgrNm(),
                basisInfo.codeHallNm(),
                basisInfo.kaptUsedate(),
                basisInfo.kaptMarea(),
                basisInfo.kaptMparea60(),
                basisInfo.kaptMparea85(),
                basisInfo.kaptMparea135(),
                basisInfo.kaptMparea136(),
                basisInfo.privArea(),
                basisInfo.bjdCode(),
                basisInfo.kaptTopFloor(),
                basisInfo.ktownFlrNo(),
                basisInfo.kaptBaseFloor(),
                basisInfo.kaptdEcntp()
        );
    }

    @Override
    public boolean existsByKaptCode(String kaptCode) {
        return jpaRepository.existsByKaptCode(kaptCode);
    }

    @Override
    @Transactional
    public int markInactiveForInactiveComplexes() {
        return jdbcTemplate.update("""
                UPDATE real_estate_apartment_complex_basis_info b
                SET active = false,
                    deleted_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE b.active = true
                  AND EXISTS (
                      SELECT 1
                      FROM real_estate_apartment_complex c
                      WHERE c.kapt_code = b.kapt_code
                        AND c.active = false
                  )
                """);
    }
}
