package cn.gugufish.utils;

import java.util.Locale;
import java.util.Set;

public final class OperationLogAuditUtils {

    private static final Set<String> INFRA_URL_EQUALS = Set.of(
            "/favicon.ico",
            "/error"
    );

    private static final Set<String> INFRA_URL_PREFIXES = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/images"
    );

    private static final Set<String> LOW_VALUE_GET_SUFFIXES = Set.of(
            "/list",
            "/summary",
            "/overview",
            "/options",
            "/status",
            "/me"
    );

    private OperationLogAuditUtils() {}

    public static boolean isInfrastructureRequest(String requestMethod, String requestUrl) {
        String method = normalizeMethod(requestMethod);
        String url = normalizeUrl(requestUrl);
        if (url == null) return false;
        if ("OPTIONS".equals(method)) return true;
        if (INFRA_URL_EQUALS.contains(url)) return true;
        for (String prefix : INFRA_URL_PREFIXES) {
            if (url.startsWith(prefix)) return true;
        }
        return false;
    }

    public static boolean isLowValueRequest(String requestMethod, String requestUrl) {
        String method = normalizeMethod(requestMethod);
        String url = normalizeUrl(requestUrl);
        if (url == null) return false;
        if (isInfrastructureRequest(method, url)) return true;
        if (!"GET".equals(method)) return false;
        for (String suffix : LOW_VALUE_GET_SUFFIXES) {
            if (url.endsWith(suffix)) return true;
        }
        return false;
    }

    private static String normalizeMethod(String requestMethod) {
        if (requestMethod == null) return "";
        return requestMethod.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeUrl(String requestUrl) {
        if (requestUrl == null) return null;
        String text = requestUrl.trim();
        return text.isEmpty() ? null : text;
    }
}
