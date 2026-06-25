package realtyos.server.application.rag.domain;

public record QueryTarget(
        String value,
        QueryTargetKind kind
) {
    public static QueryTarget from(String value) {
        return new QueryTarget(value, isRegionValue(value) ? QueryTargetKind.REGION : QueryTargetKind.ANY);
    }

    private static boolean isRegionValue(String value) {
        return value.endsWith("구")
                || value.endsWith("동")
                || value.endsWith("읍")
                || value.endsWith("면")
                || value.endsWith("리");
    }
}
