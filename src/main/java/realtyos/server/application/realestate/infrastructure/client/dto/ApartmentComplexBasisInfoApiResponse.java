package realtyos.server.application.realestate.infrastructure.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ApartmentComplexBasisInfoApiResponse {

    private Response response;
    private Header header;
    private Body body;

    public Header header() {
        if (response != null && response.header != null) {
            return response.header;
        }
        return header;
    }

    public Body body() {
        if (response != null && response.body != null) {
            return response.body;
        }
        return body;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    public static class Body {
        private Item item;
    }

    @Getter
    @NoArgsConstructor
    public static class Item {
        private String zipcode;
        private String kaptCode;
        private String kaptName;
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
        private String kaptUrl;
        private String codeAptNm;
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
}
