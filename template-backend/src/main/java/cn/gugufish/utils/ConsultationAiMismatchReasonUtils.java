package cn.gugufish.utils;

import com.alibaba.fastjson2.JSON;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConsultationAiMismatchReasonUtils {

    private static final Map<String, String> REASON_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("supplementary_information", "补充信息后判断调整");
        labels.put("clinical_judgment", "结合临床经验调整");
        labels.put("inspection_needed", "需结合线下检查结果");
        labels.put("risk_control", "出于安全考虑调整");
        labels.put("patient_preference", "结合患者意愿与依从性");
        labels.put("other", "其他原因");
        REASON_LABELS = Collections.unmodifiableMap(labels);
    }

    private ConsultationAiMismatchReasonUtils() {
    }

    public static List<String> normalizeCodes(List<String> source) {
        if (source == null || source.isEmpty()) return List.of();
        return source.stream()
                .map(ConsultationAiMismatchReasonUtils::trimToNull)
                .filter(Objects::nonNull)
                .filter(REASON_LABELS::containsKey)
                .distinct()
                .limit(6)
                .toList();
    }

    public static List<String> parseCodes(String json) {
        if (trimToNull(json) == null) return List.of();
        try {
            return normalizeCodes(JSON.parseArray(json, String.class));
        } catch (Exception exception) {
            return List.of();
        }
    }

    public static String toJson(List<String> source) {
        List<String> codes = normalizeCodes(source);
        return codes.isEmpty() ? null : JSON.toJSONString(codes);
    }

    public static String labelOf(String code) {
        return REASON_LABELS.getOrDefault(trimToNull(code), trimToNull(code));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
