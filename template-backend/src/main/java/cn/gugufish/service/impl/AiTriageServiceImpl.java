package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageAdvice;
import cn.gugufish.ai.AiTriageContext;
import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.service.AiTriageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiTriageServiceImpl implements AiTriageService {

    private final ObjectProvider<DeepSeekChatModel> chatModelProvider;
    private final AiTriageProperties properties;
    private final Environment environment;

    public AiTriageServiceImpl(ObjectProvider<DeepSeekChatModel> chatModelProvider,
                               AiTriageProperties properties,
                               Environment environment) {
        this.chatModelProvider = chatModelProvider;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && hasApiKey()
                && chatModelProvider.getIfAvailable() != null;
    }

    @Override
    public AiTriageAdvice generateInitialAdvice(AiTriageContext context) {
        if (context == null || context.getRecord() == null || !isAvailable()) return null;

        try {
            DeepSeekChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) return null;

            ChatClient chatClient = ChatClient.create(chatModel);
            AiTriageAdvice advice = chatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(context))
                    .call()
                    .entity(AiTriageAdvice.class);
            return normalizeAdvice(advice, context);
        } catch (Exception exception) {
            ConsultationRecord record = context.getRecord();
            log.warn("AI triage generation skipped for consultation {} because DeepSeek call failed: {}",
                    record == null ? null : record.getId(),
                    exception.getMessage());
            return null;
        }
    }

    @Override
    public AiTriageAdvice continueConversation(AiTriageContext context) {
        if (context == null || context.getRecord() == null || !isAvailable()) return null;
        if (!StringUtils.hasText(context.getUserMessage())) return null;

        try {
            DeepSeekChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) return null;

            ChatClient chatClient = ChatClient.create(chatModel);
            AiTriageAdvice advice = chatClient.prompt()
                    .system(buildConversationSystemPrompt())
                    .user(buildConversationUserPrompt(context))
                    .call()
                    .entity(AiTriageAdvice.class);
            return normalizeAdvice(advice, context);
        } catch (Exception exception) {
            ConsultationRecord record = context.getRecord();
            log.warn("AI triage conversation skipped for consultation {} because DeepSeek call failed: {}",
                    record == null ? null : record.getId(),
                    exception.getMessage());
            return null;
        }
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(environment.getProperty("spring.ai.deepseek.api-key"))
                || StringUtils.hasText(environment.getProperty("spring.ai.deepseek.chat.api-key"));
    }

    private String buildSystemPrompt() {
        return """
                你是互联网医疗系统中的 AI 导诊助手。
                你的职责是基于已有问诊资料生成补充性的导诊建议，帮助患者和医生更快理解当前情况。
                你不是最终诊断医生，不能输出确定性诊断结论，不能替代急诊或线下面诊。
                你必须遵守以下规则：
                1. 只根据输入资料给出建议，不要编造不存在的症状、病史或检查结果。
                2. 优先识别高风险场景，如胸痛伴呼吸困难、大出血、意识障碍、高热抽搐等；遇到明显高风险时，要提高线下就医和人工接管建议。
                3. 推荐医生时，只能从系统提供的候选医生中选择或说明没有足够依据。
                4. 输出重点是导诊总结、风险提醒、就诊方式建议、候选医生解释和建议补充问题。
                5. 输出应简洁、专业、可解释，适合直接展示在导诊记录中。
                """;
    }

    private String buildConversationSystemPrompt() {
        return """
                你是互联网医疗系统中的 AI 导诊助手，正在继续一段已开始的导诊会话。
                你需要根据既有导诊记录和患者刚刚补充的内容，生成结构化的继续导诊回复。
                你必须遵守以下规则：
                1. reply 字段用于直接回复患者，语气平和、清晰、专业，不要给出确定性诊断。
                2. 如果当前信息不足，请在 nextQuestions 中继续提出最多 3 个关键补充问题。
                3. 如果出现明显高风险，请明确建议线下就医或医生尽快接管。
                4. 推荐医生时只能从候选医生中选择。
                5. 你的输出重点是继续导诊，不要重复长篇免责声明。
                """;
    }

    private String buildUserPrompt(AiTriageContext context) {
        ConsultationRecord record = context.getRecord();
        List<ConsultationRecordAnswer> answers = context.getAnswers() == null ? List.of() : context.getAnswers();
        List<ConsultationRecommendDoctorVO> doctors = context.getDoctorCandidates() == null ? List.of() : context.getDoctorCandidates();

        String answerSummary = answers.stream()
                .filter(item -> StringUtils.hasText(item.getFieldValue()))
                .map(item -> item.getFieldLabel() + "：" + abbreviate(displayAnswer(item), 120))
                .limit(12)
                .collect(Collectors.joining("\n"));

        String doctorSummary = doctors.stream()
                .limit(Math.max(properties.getDoctorCandidateLimit(), 1))
                .map(item -> {
                    List<String> segments = new ArrayList<>();
                    segments.add("医生ID=" + item.getId());
                    if (StringUtils.hasText(item.getName())) segments.add("姓名=" + item.getName());
                    if (StringUtils.hasText(item.getTitle())) segments.add("职称=" + item.getTitle());
                    if (StringUtils.hasText(item.getExpertise())) segments.add("擅长=" + abbreviate(item.getExpertise(), 120));
                    if (item.getServiceTags() != null && !item.getServiceTags().isEmpty()) {
                        segments.add("标签=" + String.join("、", item.getServiceTags()));
                    }
                    if (StringUtils.hasText(item.getNextScheduleText())) {
                        segments.add("排班=" + item.getNextScheduleText());
                    }
                    return "- " + String.join("；", segments);
                })
                .collect(Collectors.joining("\n"));

        return """
                请基于以下问诊资料生成一份结构化 AI 导诊补充建议。

                基本信息：
                - 问诊ID：%s
                - 问诊单号：%s
                - 就诊人：%s
                - 问诊分类：%s
                - 当前匹配科室：%s
                - 当前规则分诊等级：%s
                - 当前规则建议动作：%s

                主诉：
                %s

                健康摘要：
                %s

                系统规则建议：
                - 分诊建议：%s
                - 风险摘要：%s

                问诊补充答案：
                %s

                当前候选医生：
                %s

                请注意：
                - 如果存在高风险，请把 suggestOfflineImmediately 设为 1，并把 shouldEscalateToHuman 设为 1。
                - recommendedVisitType 建议使用 offline、online、followup、emergency 之一。
                - confidenceScore 使用 0 到 1 之间的小数。
                - nextQuestions 最多给 3 条。
                - riskFlags 最多给 5 条。
                - 如果候选医生不足以判断，可以不给 recommendedDoctorIds，但要在 doctorRecommendationReason 中说明原因。
                """.formatted(
                safeNumber(record.getId()),
                safeText(record.getConsultationNo(), "-"),
                safeText(record.getPatientName(), "-"),
                safeText(record.getCategoryName(), "-"),
                safeText(record.getDepartmentName(), "未匹配"),
                safeText(record.getTriageLevelName(), "待评估"),
                safeText(record.getTriageActionType(), "待评估"),
                safeText(record.getChiefComplaint(), "暂无主诉"),
                safeText(record.getHealthSummary(), "暂无健康摘要"),
                safeText(record.getTriageSuggestion(), "暂无规则建议"),
                safeText(record.getTriageRuleSummary(), "暂无风险摘要"),
                safeText(answerSummary, "暂无补充答案"),
                safeText(doctorSummary, "暂无候选医生")
        );
    }

    private String buildConversationUserPrompt(AiTriageContext context) {
        ConsultationRecord record = context.getRecord();
        List<ConsultationRecordAnswer> answers = context.getAnswers() == null ? List.of() : context.getAnswers();
        List<ConsultationRecommendDoctorVO> doctors = context.getDoctorCandidates() == null ? List.of() : context.getDoctorCandidates();
        List<TriageMessage> messages = context.getTriageMessages() == null ? List.of() : context.getTriageMessages();

        String answerSummary = answers.stream()
                .filter(item -> StringUtils.hasText(item.getFieldValue()))
                .map(item -> item.getFieldLabel() + "：" + abbreviate(displayAnswer(item), 120))
                .limit(12)
                .collect(Collectors.joining("\n"));

        String messageHistory = messages.stream()
                .sorted((left, right) -> {
                    int leftSort = left.getSort() == null ? 0 : left.getSort();
                    int rightSort = right.getSort() == null ? 0 : right.getSort();
                    return Integer.compare(leftSort, rightSort);
                })
                .skip(Math.max(messages.size() - 12, 0))
                .map(item -> "[" + roleLabel(item.getRoleType()) + "/" + safeText(item.getMessageType(), "-") + "] "
                        + safeText(item.getContent(), "-"))
                .collect(Collectors.joining("\n"));

        String doctorSummary = doctors.stream()
                .limit(Math.max(properties.getDoctorCandidateLimit(), 1))
                .map(item -> {
                    List<String> segments = new ArrayList<>();
                    segments.add("医生ID=" + item.getId());
                    if (StringUtils.hasText(item.getName())) segments.add("姓名=" + item.getName());
                    if (StringUtils.hasText(item.getTitle())) segments.add("职称=" + item.getTitle());
                    if (StringUtils.hasText(item.getExpertise())) segments.add("擅长=" + abbreviate(item.getExpertise(), 120));
                    return "- " + String.join("；", segments);
                })
                .collect(Collectors.joining("\n"));

        return """
                请继续这段导诊会话，并根据患者刚补充的内容给出下一轮导诊回复。

                基本信息：
                - 问诊ID：%s
                - 就诊人：%s
                - 问诊分类：%s
                - 当前规则分诊等级：%s
                - 当前规则建议动作：%s
                - 当前匹配科室：%s

                主诉：
                %s

                健康摘要：
                %s

                问诊答案摘要：
                %s

                导诊历史消息：
                %s

                当前候选医生：
                %s

                患者刚补充的内容：
                %s

                输出要求：
                - reply 要直接回答患者，并结合当前导诊状态说明下一步建议。
                - 如果当前资料足够，可减少 nextQuestions。
                - 如果风险提高，请同步更新 riskFlags、recommendedVisitType、shouldEscalateToHuman、suggestOfflineImmediately。
                """.formatted(
                safeNumber(record.getId()),
                safeText(record.getPatientName(), "-"),
                safeText(record.getCategoryName(), "-"),
                safeText(record.getTriageLevelName(), "待评估"),
                safeText(record.getTriageActionType(), "待评估"),
                safeText(record.getDepartmentName(), "未匹配"),
                safeText(record.getChiefComplaint(), "暂无主诉"),
                safeText(record.getHealthSummary(), "暂无健康摘要"),
                safeText(answerSummary, "暂无问诊答案"),
                safeText(messageHistory, "暂无导诊历史"),
                safeText(doctorSummary, "暂无候选医生"),
                safeText(context.getUserMessage(), "暂无补充内容")
        );
    }

    private AiTriageAdvice normalizeAdvice(AiTriageAdvice advice, AiTriageContext context) {
        if (advice == null) return null;

        advice.setReply(trimToNull(advice.getReply()));
        advice.setSummary(trimToNull(advice.getSummary()));
        advice.setRecommendedDepartmentName(trimToNull(advice.getRecommendedDepartmentName()));
        advice.setRecommendedVisitType(normalizeVisitType(advice.getRecommendedVisitType(), context));
        advice.setDoctorRecommendationReason(trimToNull(advice.getDoctorRecommendationReason()));
        advice.setRiskFlags(normalizeList(advice.getRiskFlags(), 5, 80));
        advice.setNextQuestions(normalizeList(advice.getNextQuestions(), 3, 120));
        advice.setRecommendedDoctorNames(normalizeList(advice.getRecommendedDoctorNames(), properties.getDoctorCandidateLimit(), 40));
        advice.setRecommendedDoctorIds(normalizeDoctorIds(advice.getRecommendedDoctorIds(), context));
        advice.setShouldEscalateToHuman(normalizeBinary(advice.getShouldEscalateToHuman()));
        advice.setSuggestOfflineImmediately(normalizeBinary(advice.getSuggestOfflineImmediately()));
        advice.setConfidenceScore(normalizeScore(advice.getConfidenceScore()));

        if (!StringUtils.hasText(advice.getReply())
                && !StringUtils.hasText(advice.getSummary())
                && !StringUtils.hasText(advice.getDoctorRecommendationReason())
                && (advice.getNextQuestions() == null || advice.getNextQuestions().isEmpty())) {
            return null;
        }
        return advice;
    }

    private List<String> normalizeList(List<String> source, int limit, int textLimit) {
        if (source == null || source.isEmpty()) return List.of();
        Set<String> values = new LinkedHashSet<>();
        for (String item : source) {
            String text = trimToNull(item);
            if (text != null) values.add(abbreviate(text, textLimit));
            if (values.size() >= limit) break;
        }
        return List.copyOf(values);
    }

    private List<Integer> normalizeDoctorIds(List<Integer> doctorIds, AiTriageContext context) {
        if (doctorIds == null || doctorIds.isEmpty() || context.getDoctorCandidates() == null) return List.of();
        Set<Integer> candidateIds = context.getDoctorCandidates().stream()
                .map(ConsultationRecommendDoctorVO::getId)
                .collect(Collectors.toSet());
        return doctorIds.stream()
                .filter(candidateIds::contains)
                .distinct()
                .limit(Math.max(properties.getDoctorCandidateLimit(), 1))
                .toList();
    }

    private Integer normalizeBinary(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        if (value == null) return BigDecimal.valueOf(0.70).setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeVisitType(String value, AiTriageContext context) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return context != null && context.getRecord() != null
                    ? safeText(context.getRecord().getTriageActionType(), "online")
                    : "online";
        }
        return switch (normalized.toLowerCase()) {
            case "offline", "online", "followup", "emergency" -> normalized.toLowerCase();
            default -> context != null && context.getRecord() != null
                    ? safeText(context.getRecord().getTriageActionType(), "online")
                    : "online";
        };
    }

    private String displayAnswer(ConsultationRecordAnswer answer) {
        if ("switch".equalsIgnoreCase(answer.getFieldType())) {
            return "1".equals(answer.getFieldValue()) ? "是" : "否";
        }
        return answer.getFieldValue();
    }

    private String roleLabel(String roleType) {
        return switch (roleType == null ? "" : roleType.toLowerCase()) {
            case "user" -> "患者";
            case "assistant" -> "AI";
            case "rule_engine" -> "规则";
            case "system" -> "系统";
            default -> roleType;
        };
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String safeNumber(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(maxLength - 3, 0)) + "...";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
