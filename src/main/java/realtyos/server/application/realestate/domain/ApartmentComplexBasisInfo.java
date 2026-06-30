package realtyos.server.application.realestate.domain;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ApartmentComplexBasisInfo(
        Long id,
        String zipcode,
        String kaptCode,
        String kaptName,
        String kaptAddr,
        String codeSaleNm,
        String codeHeatNm,
        BigDecimal kaptTarea,
        Integer kaptDongCnt,
        String kaptdaCnt,
        String kaptBcompany,
        String kaptAcompany,
        String kaptTel,
        String kaptFax,
        String kaptUrl,
        String codeAptNm,
        String doroJuso,
        Integer hoCnt,
        String codeMgrNm,
        String codeHallNm,
        String kaptUsedate,
        BigDecimal kaptMarea,
        BigDecimal kaptMparea60,
        BigDecimal kaptMparea85,
        BigDecimal kaptMparea135,
        BigDecimal kaptMparea136,
        BigDecimal privArea,
        String bjdCode,
        Integer kaptTopFloor,
        Integer ktownFlrNo,
        Integer kaptBaseFloor,
        Integer kaptdEcntp
) {
}
