package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageAdvice;
import cn.gugufish.ai.AiTriageContext;
import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.ai.advisor.MedicalLongTermMemoryAdvisor;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.service.AiTriageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 导诊服务 — 基于 Spring AI Advisors API 的高阶重构版本。
 *
 * <h3>与旧版 AiTriageServiceImpl 的核心差异</h3>
 * <table>
 *   <tr><th>维度</th><th>旧版</th><th>新版（本类）</th></tr>
 *   <tr><td>ChatClient 创建方式</td>
 *       <td>每次请求 ChatClient.create(chatModel)</td>
 *       <td>全局单例 ChatClient，构造时已织入 Advisor 链</td></tr>
 *   <tr><td>多轮对话上下文</td>
 *       <td>手动拼接历史消息到 User Prompt（最多 12 条）</td>
 *       <td>MessageChatMemoryAdvisor 自动管理滑动窗口</td></tr>
 *   <tr><td>跨会话记忆</td>
 *       <td>无 → 过敏史等红旗信息会被遗忘</td>
 *       <td>MedicalLongTermMemoryAdvisor 基于 Redis 持久化</td></tr>
 *   <tr><td>防幻觉</td>
 *       <td>仅靠 System Prompt 约束</td>
 *       <td>QuestionAnswerAdvisor 检索医学知识库做 RAG 增强</td></tr>
 * </table>
 *
 * <h3>激活方式</h3>
 * <p>在 application.yml 中设置 {@code consultation.ai.triage.use-advisor=true} 启用本实现，
 * 否则使用旧版 {@link AiTriageServiceImpl} 做兜底。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "consultation.ai.triage.use-advisor", havingValue = "true")
public class AdvancedAiTriageServiceImpl implements AiTriageService {

    /**
     * 全局配置好 Advisor 链的 ChatClient 单例。
     * 由 AiAdvisorConfiguration 通过 Builder 模式组装：
     *   Advisor 链 = LongTermMemory(10) → ChatMemory(100) → RAG(200) → LLM
     */
    private final ChatClient advisorChatClient;

    private final AiTriageProperties properties;
    private final Environment environment;
    private final ConsultationDepartmentRoutingService consultationDepartmentRoutingService;

    /**
     * 注入方式说明：
     * - advisorChatClientProvider：从 AiAdvisorConfiguration 注入的全局 ChatClient
     *   （使用 ObjectProvider 避免 DeepSeek 未配置时启动失败）
     */
    public AdvancedAiTriageServiceImpl(
            ObjectProvider<ChatClient> advisorChatClientProvider,
            AiTriageProperties properties,
            Environment environment,
            ConsultationDepartmentRoutingService consultationDepartmentRoutingService) {
        this.advisorChatClient = advisorChatClientProvider.getIfAvailable();
        this.properties = properties;
        this.environment = environment;
        this.consultationDepartmentRoutingService = consultationDepartmentRoutingService;
    }

    @PostConstruct
    public void logAvailabilityAtStartup() {
        boolean triageEnabled = properties.isEnabled();
        boolean apiKeyConfigured = hasApiKey();
        boolean chatClientReady = advisorChatClient != null;
        if (triageEnabled && apiKeyConfigured && chatClientReady) {
            log.info("AI triage advisor pipeline is ready: triageEnabled={}, apiKeyConfigured={}, advisorChatClientReady={}",
                    triageEnabled, apiKeyConfigured, true);
            return;
        }
        log.warn("AI triage advisor pipeline is not ready: triageEnabled={}, apiKeyConfigured={}, advisorChatClientReady={}",
                triageEnabled, apiKeyConfigured, chatClientReady);
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && hasApiKey()
                && advisorChatClient != null;
    }

    // ==================== 初次导诊 ====================

    @Override
    public AiTriageAdvice generateInitialAdvice(AiTriageContext context) {
        if (context == null || context.getRecord() == null || !isAvailable()) return null;

        try {
            // conversationId 格式：triage-{问诊ID}，确保同一问诊单共享上下文
            String conversationId = "triage-" + context.getRecord().getId();
            // patientId 用于长期记忆跨会话召回
            String patientId = resolvePatientId(context.getRecord());

            /*
             * 关键改动：不再 ChatClient.create(chatModel).prompt()...
             * 而是直接使用已织入 Advisor 链的 advisorChatClient。
             *
             * .advisors() 在运行时传入动态参数：
             *   - CONVERSATION_ID  → 短期记忆按此 ID 隔离不同问诊单
             *   - PATIENT_ID_KEY   → 长期记忆按此 ID 读取该患者历史关键事实
             *
             * 执行流程（自动）：
             *   1) MedicalLongTermMemoryAdvisor.before → 从 Redis 读取过敏史等，注入 SystemMessage
             *   2) MessageChatMemoryAdvisor.before     → 从内存读取本问诊的历史对话，注入消息列表
             *   3) QuestionAnswerAdvisor.before        → 用 userPrompt 检索向量库，把匹配的医学知识拼入 context
             *   4) DeepSeek LLM 调用
             *   5) QuestionAnswerAdvisor.after          → 透传
             *   6) MessageChatMemoryAdvisor.after       → 把本轮 user+assistant 消息存入滑动窗口
             *   7) MedicalLongTermMemoryAdvisor.after    → 从响应中提取过敏/高风险事实，写回 Redis
             */
            AiTriageAdvice advice = advisorChatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(context))
                    .advisors(advisor -> advisor
                            .param(ChatMemory.CONVERSATION_ID, conversationId)
                            .param(MedicalLongTermMemoryAdvisor.PATIENT_ID_KEY, patientId)
                    )
                    .call()
                    .entity(AiTriageAdvice.class);

            return normalizeAdvice(advice, context);
        } catch (Exception exception) {
            ConsultationRecord record = context.getRecord();
            log.warn("AI triage generation skipped for consultation {} because call failed: {}",
                    record == null ? null : record.getId(),
                    exception.getMessage());
            return null;
        }
    }

    // ==================== 多轮继续对话 ====================

    @Override
    public AiTriageAdvice continueConversation(AiTriageContext context) {
        if (context == null || context.getRecord() == null || !isAvailable()) return null;
        if (!StringUtils.hasText(context.getUserMessage())) return null;

        try {
            String conversationId = "triage-" + context.getRecord().getId();
            String patientId = resolvePatientId(context.getRecord());

            /*
             * 与旧版最大的区别：
             *
             * 旧版需要手动拼接 messageHistory（最多 12 条截断），历史一长就丢失上下文。
             * 新版由 MessageChatMemoryAdvisor 自动管理滑动窗口（默认 20 条），
             * System Message 始终保留，用户/助手消息按 FIFO 淘汰。
             *
             * 同时 QuestionAnswerAdvisor 会自动提取用户消息中的关键词，
             * 从医学知识库检索相关指南文档，拼入 Prompt context，
             * 约束 LLM 不能脱离医学事实凭空编造诊断建议。
             *
             * 此外注意 buildConversationUserPrompt 不再手动拼接 triageMessages，
             * 因为 Advisor 已经自动管理对话历史了。
             */
            AiTriageAdvice advice = advisorChatClient.prompt()
                    .system(buildConversationSystemPrompt())
                    .user(buildConversationUserPrompt(context))
                    .advisors(advisor -> advisor
                            .param(ChatMemory.CONVERSATION_ID, conversationId)
                            .param(MedicalLongTermMemoryAdvisor.PATIENT_ID_KEY, patientId)
                    )
                    .call()
                    .entity(AiTriageAdvice.class);

            return normalizeAdvice(advice, context);
        } catch (Exception exception) {
            ConsultationRecord record = context.getRecord();
            log.warn("AI triage conversation skipped for consultation {} because call failed: {}",
                    record == null ? null : record.getId(),
                    exception.getMessage());
            return null;
        }
    }

    // ==================== 患者 ID 解析（用于长期记忆 Redis key） ====================

    private String resolvePatientId(ConsultationRecord record) {
        // 优先用就诊人姓名的 hashCode 作为长期记忆 key
        // 实际生产环境建议替换为患者的唯一业务 ID
        if (record.getPatientName() != null) {
            return "patient-" + record.getPatientName().hashCode();
        }
        return "patient-" + record.getId();
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(environment.getProperty("spring.ai.deepseek.api-key"))
                || StringUtils.hasText(environment.getProperty("spring.ai.deepseek.chat.api-key"));
    }

    // ==================== System Prompt ====================
    // 相比旧版新增了第 6、7 条规则，用于引导 LLM 正确使用 Advisor 注入的额外上下文

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
                6. 如果系统提供了"患者长期医学记忆"，你必须将其中的过敏史和红旗症状纳入分析，不可忽略。
                7. 如果系统提供了"相关医学知识参考"，请基于这些参考资料约束你的建议范围，不要超越参考资料给出无根据的诊疗建议。
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
                6. 如果系统注入了患者的长期医学记忆（如过敏史），你必须在回复中体现对这些信息的考量。
                7. 基于系统提供的医学知识参考材料回答，不要超出参考范围做主观臆断。
                """;
    }

    // ==================== User Prompt（保持原有结构化格式） ====================

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
        String departmentGuidance = consultationDepartmentRoutingService.buildDepartmentSelectionGuidance(record);

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

                科室判断要求：
                %s

                请注意：
                - 如果存在高风险，请把 suggestOfflineImmediately 设为 1，并把 shouldEscalateToHuman 设为 1。
                - recommendedVisitType 建议使用 offline、online、followup、emergency 之一。
                - recommendedDepartmentName 必须返回项目中真实存在的科室名称；如果无法判断更合适的专科，请返回“全科门诊”。
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
                safeText(doctorSummary, "暂无候选医生"),
                safeText(departmentGuidance, "如能判断更合适的目标科室，请在 recommendedDepartmentName 中返回系统真实科室名称。")
        );
    }

    private String buildConversationUserPrompt(AiTriageContext context) {
        ConsultationRecord record = context.getRecord();
        List<ConsultationRecordAnswer> answers = context.getAnswers() == null ? List.of() : context.getAnswers();
        List<ConsultationRecommendDoctorVO> doctors = context.getDoctorCandidates() == null ? List.of() : context.getDoctorCandidates();
        // 注意：不再手动拼接 triageMessages / messageHistory
        // MessageChatMemoryAdvisor 已自动管理对话滑动窗口

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
                    return "- " + String.join("；", segments);
                })
                .collect(Collectors.joining("\n"));
        String departmentGuidance = consultationDepartmentRoutingService.buildDepartmentSelectionGuidance(record);

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

                当前候选医生：
                %s

                患者刚补充的内容：
                %s

                科室判断要求：
                %s

                输出要求：
                - reply 要直接回答患者，并结合当前导诊状态说明下一步建议。
                - 如果当前资料足够，可减少 nextQuestions。
                - 如果风险提高，请同步更新 riskFlags、recommendedVisitType、shouldEscalateToHuman、suggestOfflineImmediately。
                - recommendedDepartmentName 必须使用项目真实科室名称；如果仍然无法判断更合适的专科，请返回“全科门诊”。
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
                safeText(doctorSummary, "暂无候选医生"),
                safeText(context.getUserMessage(), "暂无补充内容"),
                safeText(departmentGuidance, "如能判断更合适的目标科室，请在 recommendedDepartmentName 中返回系统真实科室名称。")
        );
    }

    // ==================== 规范化逻辑（与原 AiTriageServiceImpl 完全一致） ====================

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
