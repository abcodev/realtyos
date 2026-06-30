package realtyos.server.application.realestate.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import realtyos.server.application.common.entity.BaseEntity;

import java.math.BigDecimal;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "real_estate_apartment_complex_basis_info")
public class ApartmentComplexBasisInfoJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String zipcode;

    @Column(nullable = false, unique = true, length = 30)
    private String kaptCode;

    @Column(nullable = false, length = 255)
    private String kaptName;

    @Column(length = 500)
    private String kaptAddr;

    private String codeSaleNm;
    private String codeHeatNm;
    private BigDecimal kaptTarea;
    private Integer kaptDongCnt;
    private String kaptdaCnt;
    private String kaptBcompany;
    private String kaptAcompany;
    private String kaptTel;
    private String kaptFax;

    @Column(length = 1000)
    private String kaptUrl;

    private String codeAptNm;

    @Column(length = 500)
    private String doroJuso;

    private Integer hoCnt;
    private String codeMgrNm;
    private String codeHallNm;
    private String kaptUsedate;
    private BigDecimal kaptMarea;
    private BigDecimal kaptMparea60;
    private BigDecimal kaptMparea85;
    private BigDecimal kaptMparea135;
    private BigDecimal kaptMparea136;
    private BigDecimal privArea;
    private String bjdCode;
    private Integer kaptTopFloor;
    private Integer ktownFlrNo;
    private Integer kaptBaseFloor;
    private Integer kaptdEcntp;
}
