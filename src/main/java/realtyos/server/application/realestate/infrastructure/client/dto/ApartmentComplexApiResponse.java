package realtyos.server.application.realestate.infrastructure.client.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
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
        @JsonDeserialize(using = ItemsDeserializer.class)
        private Items items;
        private String numOfRows;
        private String pageNo;
        private String totalCount;

        public List<Item> itemList() {
            if (items == null || items.item == null) {
                return Collections.emptyList();
            }
            return items.item;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Items {
        private List<Item> item;

        private Items(List<Item> item) {
            this.item = item;
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

    public static class ItemsDeserializer extends JsonDeserializer<Items> {

        @Override
        public Items deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            List<Item> items = new ArrayList<>();

            JsonNode itemNode = node.has("item") ? node.get("item") : node;
            if (itemNode == null || itemNode.isNull()) {
                return new Items(items);
            }

            if (itemNode.isArray()) {
                for (JsonNode child : itemNode) {
                    items.add(parser.getCodec().treeToValue(child, Item.class));
                }
                return new Items(items);
            }

            items.add(parser.getCodec().treeToValue(itemNode, Item.class));
            return new Items(items);
        }
    }
}
