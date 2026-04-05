package cn.gugufish.utils;

import cn.gugufish.entity.vo.response.ConsultationAiComparisonVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;
import cn.gugufish.entity.vo.response.TriageResultVO;
import cn.gugufish.entity.vo.response.TriageSessionVO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConsultationAiComparisonUtils {

    private ConsultationAiComparisonUtils() {}

    public static ConsultationAiComparisonVO build(ConsultationDoctorConclusionVO doctorConclusion,
                                                   TriageSessionVO triageSession,
                                                   TriageResultVO triageResult) {
        AiReference ai = resolveAiReference(triageSession, triageResult);
        if (!ai.hasReference()) return null;

        ConsultationAiComparisonVO vo = new ConsultationAiComparisonVO();
        vo.setAiConditionLevel(ai.conditionLevel());
        vo.setAiDisposition(ai.disposition());
        vo.setAiDepartmentName(ai.departmentName());
        vo.setAiReasonText(ai.reasonText());
        vo.setAiConfidenceText(ai.confidenceText());
        vo.setAiFollowUpText(ai.followUpText());
        vo.setAiRiskFlags(ai.riskFlags());
        vo.setAiRecommendedDoctors(ai.recommendedDoctors());

        String doctorConditionLevel = trimToNull(doctorConclusion == null ? null : doctorConclusion.getConditionLevel());
        String doctorDisposition = trimToNull(doctorConclusion == null ? null : doctorConclusion.getDisposition());
        String doctorFollowUpText = buildFollowUpText(
                doctorConclusion == null ? null : doctorConclusion.getNeedFollowUp(),
                doctorConclusion == null ? null : doctorConclusion.getFollowUpWithinDays(),
                doctorConclusion != null && (
                        doctorConclusion.getNeedFollowUp() != null
                                || doctorConclusion.getFollowUpWithinDays() != null
                )
        );

        vo.setConditionLevelStatus(compareField(ai.conditionLevel(), doctorConditionLevel));
        vo.setDispositionStatus(compareField(ai.disposition(), doctorDisposition));
        vo.setFollowUpStatus(compareField(ai.followUpText(), doctorFollowUpText));
        vo.setOverallStatus(resolveOverallStatus(
                vo.getConditionLevelStatus(),
                vo.getDispositionStatus(),
                vo.getFollowUpStatus()
        ));
        vo.setSummary(buildSummary(vo.getOverallStatus(), doctorConclusion != null));
        return vo;
    }

    private static AiReference resolveAiReference(TriageSessionVO triageSession,
                                                  TriageResultVO triageResult) {
        JSONObject payload = latestAiStructuredContent(triageSession);
        String triageActionType = trimToNull(triageSession == null ? null : triageSession.getTriageActionType());
        String triageLevelSource = firstNonBlank(
                triageResult == null ? null : triageResult.getTriageLevelCode(),
                triageSession == null ? null : triageSession.getTriageLevelCode(),
                triageResult == null ? null : triageResult.getTriageLevelName(),
                triageSession == null ? null : triageSession.getTriageLevelName()
        );
        String conditionLevel = mapTriageLevelToConditionLevel(triageLevelSource);
        String disposition = mapVisitTypeToDisposition(firstNonBlank(
                payload == null ? null : payload.getString("recommendedVisitType"),
                triageActionType
        ));
        String departmentName = firstNonBlank(
                payload == null ? null : payload.getString("recommendedDepartmentName"),
                triageResult == null ? null : triageResult.getDepartmentName(),
                triageSession == null ? null : triageSession.getDepartmentName()
        );
        String reasonText = firstNonBlank(
                payload == null ? null : payload.getString("doctorRecommendationReason"),
                triageResult == null ? null : triageResult.getReasonText()
        );
        String confidenceText = buildConfidenceText(
                payload == null ? null : payload.getBigDecimal("confidenceScore"),
                triageResult == null ? null : triageResult.getConfidenceScore()
        );
        List<String> riskFlags = resolveRiskFlags(payload, triageResult);
        List<String> recommendedDoctors = resolveRecommendedDoctors(payload, triageResult);

        Integer needFollowUp = "followup".equalsIgnoreCase(firstNonBlank(
                payload == null ? null : payload.getString("recommendedVisitType"),
                triageActionType
        )) ? 1 : 0;
        String followUpText = buildFollowUpText(needFollowUp, needFollowUp == 1 ? 3 : null, disposition != null);

        boolean hasReference = StringUtils.hasText(conditionLevel)
                || StringUtils.hasText(disposition)
                || StringUtils.hasText(departmentName)
                || StringUtils.hasText(reasonText)
                || StringUtils.hasText(confidenceText)
                || StringUtils.hasText(followUpText)
                || !riskFlags.isEmpty()
                || !recommendedDoctors.isEmpty();
        return new AiReference(
                hasReference,
                conditionLevel,
                disposition,
                departmentName,
                reasonText,
                confidenceText,
                followUpText,
                riskFlags,
                recommendedDoctors
        );
    }

    private static JSONObject latestAiStructuredContent(TriageSessionVO triageSession) {
        if (triageSession == null || triageSession.getMessages() == null || triageSession.getMessages().isEmpty()) return null;
        for (int i = triageSession.getMessages().size() - 1; i >= 0; i--) {
            String roleType = trimToNull(triageSession.getMessages().get(i).getRoleType());
            String structuredContent = trimToNull(triageSession.getMessages().get(i).getStructuredContent());
            if (!"assistant".equalsIgnoreCase(roleType) || structuredContent == null) continue;
            try {
                return JSON.parseObject(structuredContent);
            } catch (Exception ignored) {
                continue;
            }
        }
        return null;
    }

    private static List<String> resolveRiskFlags(JSONObject payload, TriageResultVO triageResult) {
        List<String> list = normalizeStringList(payload == null ? null : payload.getJSONArray("riskFlags"));
        if (!list.isEmpty()) return list;
        return normalizeStringList(triageResult == null ? null : triageResult.getRiskFlagsJson());
    }

    private static List<String> resolveRecommendedDoctors(JSONObject payload, TriageResultVO triageResult) {
        List<String> names = normalizeStringList(payload == null ? null : payload.getJSONArray("recommendedDoctorNames"));
        if (!names.isEmpty()) return names;
        List<String> ids = normalizeStringList(payload == null ? null : payload.getJSONArray("recommendedDoctorIds"))
                .stream()
                .map(item -> "医生ID " + item)
                .limit(5)
                .toList();
        if (!ids.isEmpty()) return ids;

        List<String> fallback = new ArrayList<>();
        String doctorName = trimToNull(triageResult == null ? null : triageResult.getDoctorName());
        if (doctorName != null) fallback.add(doctorName);

        if (triageResult != null && StringUtils.hasText(triageResult.getDoctorCandidatesJson())) {
            try {
                JSONArray array = JSON.parseArray(triageResult.getDoctorCandidatesJson());
                for (Object item : array) {
                    if (!(item instanceof JSONObject object)) continue;
                    String name = trimToNull(object.getString("name"));
                    if (name != null && !fallback.contains(name)) fallback.add(name);
                    if (fallback.size() >= 5) break;
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private static List<String> normalizeStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof JSONArray array) {
            for (Object item : array) {
                String text = trimToNull(item == null ? null : String.valueOf(item));
                if (text != null && !result.contains(text)) result.add(text);
                if (result.size() >= 5) break;
            }
            return result;
        }
        if (value instanceof String text) {
            try {
                return normalizeStringList(JSON.parseArray(text));
            } catch (Exception ignored) {
                String item = trimToNull(text);
                return item == null ? List.of() : List.of(item);
            }
        }
        return result;
    }

    private static String mapVisitTypeToDisposition(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        return switch (text.toLowerCase()) {
            case "emergency" -> "emergency";
            case "offline" -> "offline_visit";
            case "followup", "online" -> "online_followup";
            default -> null;
        };
    }

    private static String mapTriageLevelToConditionLevel(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        String upper = text.toUpperCase();
        if (upper.contains("EMERGENCY") || upper.contains("CRITICAL") || upper.contains("RED") || text.contains("危")) return "critical";
        if (upper.contains("OFFLINE") || upper.contains("HIGH") || upper.contains("ORANGE") || text.contains("高")) return "high";
        if (upper.contains("FOLLOWUP") || upper.contains("MEDIUM") || upper.contains("YELLOW") || text.contains("中")) return "medium";
        if (upper.contains("ONLINE") || upper.contains("LOW") || upper.contains("GREEN") || text.contains("低") || text.contains("轻")) return "low";
        return null;
    }

    private static String buildFollowUpText(Integer needFollowUp, Integer days, boolean hasValue) {
        if (!hasValue) return null;
        if (needFollowUp != null && needFollowUp == 1) {
            return days != null ? days + " 天内随访" : "需要随访";
        }
        return "暂不需要随访";
    }

    private static String buildConfidenceText(BigDecimal payloadConfidence, BigDecimal resultConfidence) {
        BigDecimal value = payloadConfidence != null ? payloadConfidence : resultConfidence;
        if (value == null || value.signum() <= 0) return null;
        return value.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP) + "%";
    }

    private static String compareField(String aiValue, String doctorValue) {
        if (!StringUtils.hasText(aiValue) && !StringUtils.hasText(doctorValue)) return "pending";
        if (!StringUtils.hasText(aiValue) || !StringUtils.hasText(doctorValue)) return "pending";
        return Objects.equals(aiValue, doctorValue) ? "match" : "mismatch";
    }

    private static String resolveOverallStatus(String conditionLevelStatus,
                                               String dispositionStatus,
                                               String followUpStatus) {
        List<String> statuses = List.of(conditionLevelStatus, dispositionStatus, followUpStatus);
        if (statuses.stream().allMatch("pending"::equals)) return "pending";
        if (statuses.stream().anyMatch("mismatch"::equals)) return "mismatch";
        if (statuses.stream().anyMatch("pending"::equals)) return "partial";
        return "match";
    }

    private static String buildSummary(String overallStatus, boolean hasDoctorConclusion) {
        return switch (overallStatus) {
            case "match" -> "医生最终结论与 AI 建议整体一致，可作为后续复诊和复盘参考。";
            case "mismatch" -> "医生最终结论与 AI 建议存在差异，最终以医生人工判断为准。";
            case "partial" -> hasDoctorConclusion
                    ? "医生已给出最终结论，但当前仅部分字段可与 AI 建议进行对比。"
                    : "当前先展示 AI 建议供参考，医生结论仍在持续补充中。";
            default -> "当前先展示 AI 建议供参考，医生尚未形成最终结构化结论。";
        };
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String text = trimToNull(value);
            if (text != null) return text;
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private record AiReference(boolean hasReference,
                               String conditionLevel,
                               String disposition,
                               String departmentName,
                               String reasonText,
                               String confidenceText,
                               String followUpText,
                               List<String> riskFlags,
                               List<String> recommendedDoctors) {
    }
}
