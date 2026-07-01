package realtyos.server.application.realestate.infrastructure.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class ApartmentComplexApiResponse {

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
        private List<Item> items;
        private String numOfRows;
        private String pageNo;
        private String totalCount;

        public List<Item> itemList() {
            if (items == null) {
                return Collections.emptyList();
            }
            return items;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Item {
        private String kaptCode;
        private String kaptName;
        private String as1;
        private String as2;
        private String as3;
        private String as4;
        private String bjdCode;
    }
}
