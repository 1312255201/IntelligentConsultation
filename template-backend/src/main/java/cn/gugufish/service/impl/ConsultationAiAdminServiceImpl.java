package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.DoctorFormAiLog;
import cn.gugufish.entity.dto.DoctorMessageAiLog;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsageFieldVO;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsageItemVO;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsageOverviewVO;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsagePromptVO;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsageSceneVO;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsageTemplateVO;
import cn.gugufish.entity.vo.response.AdminDoctorMessageAiUsageItemVO;
import cn.gugufish.entity.vo.response.AdminDoctorMessageAiUsageOverviewVO;
import cn.gugufish.entity.vo.response.AdminDoctorMessageAiUsageSceneVO;
import cn.gugufish.entity.vo.response.ConsultationAiAuditItemVO;
import cn.gugufish.entity.vo.response.ConsultationAiOverviewVO;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.mapper.DoctorFormAiLogMapper;
import cn.gugufish.mapper.DoctorMessageAiLogMapper;
import cn.gugufish.mapper.TriageMessageMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.AiTriageService;
import cn.gugufish.service.ConsultationAiAdminService;
import cn.gugufish.utils.ConsultationAiMismatchReasonUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsultationAiAdminServiceImpl implements ConsultationAiAdminService {

    private static final List<String> AI_MESSAGE_TYPES = List.of(
            "ai_triage_summary",
            "ai_followup_questions",
            "ai_chat_reply"
    );

    private final ConsultationRecordMapper consultationRecordMapper;
    private final ConsultationDoctorHandleMapper consultationDoctorHandleMapper;
    private final ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper;
    private final TriageSessionMapper triageSessionMapper;
    private final TriageResultMapper triageResultMapper;
    private final TriageMessageMapper triageMessageMapper;
    private final DoctorFormAiLogMapper doctorFormAiLogMapper;
    private final DoctorMessageAiLogMapper doctorMessageAiLogMapper;
    private final AiTriageProperties properties;
    private final AiTriageService aiTriageService;
    private final ObjectProvider<DeepSeekChatModel> chatModelProvider;
    private final Environment environment;

    public ConsultationAiAdminServiceImpl(ConsultationRecordMapper consultationRecordMapper,
                                          ConsultationDoctorHandleMapper consultationDoctorHandleMapper,
                                          ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper,
                                          TriageSessionMapper triageSessionMapper,
                                          TriageResultMapper triageResultMapper,
                                          TriageMessageMapper triageMessageMapper,
                                          DoctorFormAiLogMapper doctorFormAiLogMapper,
                                          DoctorMessageAiLogMapper doctorMessageAiLogMapper,
                                          AiTriageProperties properties,
                                          AiTriageService aiTriageService,
                                          ObjectProvider<DeepSeekChatModel> chatModelProvider,
                                          Environment environment) {
        this.consultationRecordMapper = consultationRecordMapper;
        this.consultationDoctorHandleMapper = consultationDoctorHandleMapper;
        this.consultationDoctorConclusionMapper = consultationDoctorConclusionMapper;
        this.triageSessionMapper = triageSessionMapper;
        this.triageResultMapper = triageResultMapper;
        this.triageMessageMapper = triageMessageMapper;
        this.doctorFormAiLogMapper = doctorFormAiLogMapper;
        this.doctorMessageAiLogMapper = doctorMessageAiLogMapper;
        this.properties = properties;
        this.aiTriageService = aiTriageService;
        this.chatModelProvider = chatModelProvider;
        this.environment = environment;
    }

    @Override
    public ConsultationAiOverviewVO overview() {
        boolean triageEnabled = properties.isEnabled();
        boolean providerEnabled = environment.getProperty("spring.ai.deepseek.chat.enabled", Boolean.class, false);
        boolean apiKeyConfigured = hasApiKey();
        boolean modelBeanReady = chatModelProvider.getIfAvailable() != null;
        boolean runtimeAvailable = aiTriageService.isAvailable();

        ConsultationAiOverviewVO vo = new ConsultationAiOverviewVO();
        vo.setProviderName("DeepSeek");
        vo.setProviderBaseUrl(environment.getProperty("spring.ai.deepseek.base-url", "https://api.deepseek.com"));
        vo.setModelName(environment.getProperty("spring.ai.deepseek.chat.options.model", "deepseek-chat"));
        vo.setTemperature(environment.getProperty("spring.ai.deepseek.chat.options.temperature", Double.class, 0.2D));
        vo.setMaxTokens(environment.getProperty("spring.ai.deepseek.chat.options.max-tokens", Integer.class, 1200));
        vo.setPromptVersion(properties.getPromptVersion());
        vo.setDoctorCandidateLimit(properties.getDoctorCandidateLimit());
        vo.setTriageEnabled(triageEnabled);
        vo.setProviderEnabled(providerEnabled);
        vo.setApiKeyConfigured(apiKeyConfigured);
        vo.setModelBeanReady(modelBeanReady);
        vo.setRuntimeAvailable(runtimeAvailable);
        vo.setRuntimeStatus(resolveRuntimeStatus(triageEnabled, providerEnabled, apiKeyConfigured, modelBeanReady, runtimeAvailable));
        vo.setWarnings(buildWarnings(triageEnabled, providerEnabled, apiKeyConfigured, modelBeanReady));
        vo.setConsultationCount(consultationRecordMapper.selectCount(Wrappers.<ConsultationRecord>query()));
        vo.setTriageSessionCount(triageSessionMapper.selectCount(Wrappers.<TriageSession>query()));
        vo.setOpenSessionCount(triageSessionMapper.selectCount(Wrappers.<TriageSession>query().ne("status", "closed")));
        vo.setTriageResultCount(triageResultMapper.selectCount(Wrappers.<TriageResult>query()));
        vo.setAiSummaryMessageCount(countMessagesByType("ai_triage_summary"));
        vo.setAiFollowupQuestionCount(countMessagesByType("ai_followup_questions"));
        vo.setAiChatReplyCount(countMessagesByType("ai_chat_reply"));
        vo.setUserFollowupMessageCount(countMessagesByType("ai_user_followup"));
        vo.setLatestAiMessageTime(resolveLatestAiMessageTime());
        return vo;
    }

    @Override
    public AdminDoctorMessageAiUsageOverviewVO doctorMessageUsageOverview() {
        List<DoctorMessageAiLog> logs = doctorMessageAiLogMapper.selectList(Wrappers.<DoctorMessageAiLog>query()
                .orderByDesc("generated_time")
                .orderByDesc("id"));

        AdminDoctorMessageAiUsageOverviewVO overview = new AdminDoctorMessageAiUsageOverviewVO();
        overview.setGeneratedCount(logs.size());
        overview.setAppliedCount((int) logs.stream().filter(item -> defaultInt(item.getApplyCount()) > 0).count());
        overview.setSentCount((int) logs.stream().filter(item -> Objects.equals(item.getSentStatus(), 1)).count());
        overview.setTemplateUsedCount((int) logs.stream().filter(item -> Objects.equals(item.getTemplateUsed(), 1)).count());
        overview.setDeepseekCount((int) logs.stream().filter(item -> !Objects.equals(item.getFallback(), 1)
                && "deepseek".equalsIgnoreCase(trimToNull(item.getSource()))).count());
        overview.setFallbackCount((int) logs.stream().filter(item -> Objects.equals(item.getFallback(), 1)
                || "fallback".equalsIgnoreCase(trimToNull(item.getSource()))).count());
        overview.setApplyRate(resolveRate(overview.getAppliedCount(), overview.getGeneratedCount()));
        overview.setSendAdoptionRate(resolveRate(overview.getSentCount(), overview.getGeneratedCount()));
        overview.setSceneBreakdown(buildDoctorMessageSceneBreakdown(logs));
        overview.setRecentItems(logs.stream().limit(8).map(this::toDoctorMessageUsageItem).toList());
        return overview;
    }

    @Override
    public AdminDoctorFormAiUsageOverviewVO doctorFormUsageOverview() {
        List<DoctorFormAiLog> logs = doctorFormAiLogMapper.selectList(Wrappers.<DoctorFormAiLog>query()
                .orderByDesc("generated_time")
                .orderByDesc("id"));

        AdminDoctorFormAiUsageOverviewVO overview = new AdminDoctorFormAiUsageOverviewVO();
        overview.setGeneratedCount(logs.size());
        overview.setFullGeneratedCount((int) logs.stream().filter(item -> trimToNull(item.getRegenerateField()) == null).count());
        overview.setFieldRegenerateCount((int) logs.stream().filter(item -> trimToNull(item.getRegenerateField()) != null).count());
        overview.setAppliedCount((int) logs.stream().filter(item -> defaultInt(item.getApplyCount()) > 0).count());
        overview.setPureAiAppliedCount((int) logs.stream().filter(item -> defaultInt(item.getApplyCount()) > 0
                && !Objects.equals(item.getTemplateUsed(), 1)).count());
        overview.setTemplateUsedCount((int) logs.stream().filter(item -> defaultInt(item.getApplyCount()) > 0
                && Objects.equals(item.getTemplateUsed(), 1)).count());
        overview.setSavedCount((int) logs.stream().filter(item -> Objects.equals(item.getSavedStatus(), 1)).count());
        overview.setDeepseekCount((int) logs.stream().filter(item -> !Objects.equals(item.getFallback(), 1)
                && "deepseek".equalsIgnoreCase(trimToNull(item.getSource()))).count());
        overview.setFallbackCount((int) logs.stream().filter(item -> Objects.equals(item.getFallback(), 1)
                || "fallback".equalsIgnoreCase(trimToNull(item.getSource()))).count());
        overview.setContextRewriteCount((int) logs.stream().filter(item -> Objects.equals(item.getDraftContextUsed(), 1)).count());
        overview.setContextRewriteSavedCount((int) logs.stream().filter(item -> Objects.equals(item.getDraftContextUsed(), 1)
                && Objects.equals(item.getSavedStatus(), 1)).count());
        overview.setApplyRate(resolveRate(overview.getAppliedCount(), overview.getGeneratedCount()));
        overview.setSaveAdoptionRate(resolveRate(overview.getSavedCount(), overview.getGeneratedCount()));
        overview.setContextRewriteSaveRate(resolveRate(overview.getContextRewriteSavedCount(), overview.getContextRewriteCount()));
        overview.setSceneBreakdown(buildDoctorFormSceneBreakdown(logs));
        overview.setPromptBreakdown(buildDoctorFormPromptBreakdown(logs));
        overview.setFieldBreakdown(buildDoctorFormFieldBreakdown(logs));
        overview.setTemplateBreakdown(buildDoctorFormTemplateBreakdown(logs));
        overview.setRecentItems(logs.stream().limit(8).map(this::toDoctorFormUsageItem).toList());
        return overview;
    }

    @Override
    public List<ConsultationAiAuditItemVO> auditList(String messageType, String keyword, boolean highRiskOnly, int limit) {
        List<String> auditTypes = resolveAuditTypes(messageType);
        if (auditTypes.isEmpty()) return List.of();

        int safeLimit = Math.max(limit, 1);
        int candidateWindow = resolveAuditCandidateWindow(safeLimit, keyword, highRiskOnly);
        return filterAuditItems(
                buildAuditItems(loadAuditMessages(auditTypes, candidateWindow)),
                trimToNull(keyword),
                highRiskOnly,
                safeLimit
        );
    }

    @Override
    public byte[] exportAuditListCsv(String messageType, String keyword, boolean highRiskOnly, int limit) {
        return buildAuditItemsCsv(auditList(messageType, keyword, highRiskOnly, limit));
    }

    @Override
    public List<ConsultationAiAuditItemVO> highRiskReviewQueue(String keyword, int limit) {
        int safeLimit = Math.max(limit, 1);
        String keywordText = trimToNull(keyword);
        int candidateWindow = resolveReviewQueueCandidateWindow(safeLimit, keywordText);
        List<ConsultationAiAuditItemVO> auditItems = buildAuditItems(loadAuditMessages(AI_MESSAGE_TYPES, candidateWindow));
        if (auditItems.isEmpty()) return List.of();

        List<ConsultationAiAuditItemVO> result = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (ConsultationAiAuditItemVO item : auditItems) {
            if (!isHighRisk(item) || !requiresReview(item) || !matchesAuditKeyword(item, keywordText)) continue;
            String key = item.getConsultationId() == null ? "message-" + item.getMessageId() : "consultation-" + item.getConsultationId();
            if (!seenKeys.add(key)) continue;
            result.add(item);
            if (result.size() >= safeLimit) break;
        }
        return result;
    }

    @Override
    public byte[] exportHighRiskReviewQueueCsv(String keyword, int limit) {
        return buildAuditItemsCsv(highRiskReviewQueue(keyword, limit));
    }

    private Long countMessagesByType(String type) {
        return triageMessageMapper.selectCount(Wrappers.<TriageMessage>query().eq("message_type", type));
    }

    private Date resolveLatestAiMessageTime() {
        TriageMessage latestMessage = triageMessageMapper.selectOne(Wrappers.<TriageMessage>query()
                .in("message_type", AI_MESSAGE_TYPES)
                .orderByDesc("create_time")
                .orderByDesc("id")
                .last("limit 1"));
        return latestMessage == null ? null : latestMessage.getCreateTime();
    }

    private List<String> resolveAuditTypes(String messageType) {
        if (!StringUtils.hasText(messageType) || "all".equalsIgnoreCase(messageType.trim())) {
            return AI_MESSAGE_TYPES;
        }
        String normalized = messageType.trim().toLowerCase();
        return AI_MESSAGE_TYPES.contains(normalized) ? List.of(normalized) : List.of();
    }

    private List<TriageMessage> loadAuditMessages(List<String> messageTypes, int limit) {
        if (messageTypes == null || messageTypes.isEmpty() || limit <= 0) return List.of();
        List<TriageMessage> messages = triageMessageMapper.selectList(Wrappers.<TriageMessage>query()
                .in("message_type", messageTypes)
                .orderByDesc("create_time")
                .orderByDesc("id")
                .last("limit " + limit));
        return messages == null ? List.of() : messages;
    }

    private List<ConsultationAiAuditItemVO> buildAuditItems(List<TriageMessage> messages) {
        if (messages == null || messages.isEmpty()) return List.of();

        Map<Integer, TriageSession> sessionMap = loadSessionMap(messages);
        Map<Integer, ConsultationRecord> recordMap = loadRecordMap(sessionMap.values());
        Map<Integer, ConsultationDoctorHandle> doctorHandleMap = loadDoctorHandleMap(recordMap.keySet());
        Map<Integer, ConsultationDoctorConclusion> doctorConclusionMap = loadDoctorConclusionMap(recordMap.keySet());

        return messages.stream()
                .map(message -> {
                    TriageSession session = sessionMap.get(message.getSessionId());
                    Integer consultationId = firstNonNull(
                            session == null ? null : session.getConsultationId(),
                            null
                    );
                    ConsultationRecord record = consultationId == null ? null : recordMap.get(consultationId);
                    ConsultationDoctorHandle doctorHandle = consultationId == null ? null : doctorHandleMap.get(consultationId);
                    ConsultationDoctorConclusion doctorConclusion = consultationId == null ? null : doctorConclusionMap.get(consultationId);
                    return toAuditItem(message, session, record, doctorHandle, doctorConclusion);
                })
                .toList();
    }

    private List<ConsultationAiAuditItemVO> filterAuditItems(List<ConsultationAiAuditItemVO> items,
                                                             String keyword,
                                                             boolean highRiskOnly,
                                                             int limit) {
        if (items == null || items.isEmpty() || limit <= 0) return List.of();

        return items.stream()
                .filter(item -> matchesAuditKeyword(item, keyword))
                .filter(item -> !highRiskOnly || isHighRisk(item))
                .limit(limit)
                .toList();
    }

    private int resolveAuditCandidateWindow(int limit, String keyword, boolean highRiskOnly) {
        int multiplier = highRiskOnly ? 12 : 6;
        if (trimToNull(keyword) != null) {
            multiplier = Math.max(multiplier, 10);
        }
        return Math.min(Math.max(limit * multiplier, 80), 600);
    }

    private int resolveReviewQueueCandidateWindow(int limit, String keyword) {
        int multiplier = trimToNull(keyword) == null ? 10 : 16;
        return Math.min(Math.max(limit * multiplier, 80), 600);
    }

    private boolean matchesAuditKeyword(ConsultationAiAuditItemVO item, String keyword) {
        String keywordText = trimToNull(keyword);
        if (keywordText == null) return true;
        return containsIgnoreCase(item.getConsultationNo(), keywordText)
                || containsIgnoreCase(item.getSessionNo(), keywordText)
                || containsIgnoreCase(item.getPatientName(), keywordText)
                || containsIgnoreCase(item.getCategoryName(), keywordText)
                || containsIgnoreCase(item.getDepartmentName(), keywordText)
                || containsIgnoreCase(item.getTitle(), keywordText)
                || containsIgnoreCase(item.getContent(), keywordText)
                || containsIgnoreCase(resolveAuditDigest(item), keywordText)
                || containsIgnoreCase(resolveRiskFlagsText(item), keywordText)
                || containsIgnoreCase(renderAuditMessageType(item.getMessageType()), keywordText);
    }

    private Map<Integer, TriageSession> loadSessionMap(List<TriageMessage> messages) {
        Set<Integer> sessionIds = messages.stream()
                .map(TriageMessage::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (sessionIds.isEmpty()) return Map.of();

        List<TriageSession> sessions = triageSessionMapper.selectBatchIds(sessionIds);
        if (sessions == null || sessions.isEmpty()) return Map.of();
        return sessions.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TriageSession::getId, Function.identity()));
    }

    private Map<Integer, ConsultationRecord> loadRecordMap(java.util.Collection<TriageSession> sessions) {
        Set<Integer> consultationIds = sessions.stream()
                .map(TriageSession::getConsultationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (consultationIds.isEmpty()) return Map.of();

        List<ConsultationRecord> records = consultationRecordMapper.selectBatchIds(consultationIds);
        if (records == null || records.isEmpty()) return Map.of();
        return records.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ConsultationRecord::getId, Function.identity()));
    }

    private Map<Integer, ConsultationDoctorHandle> loadDoctorHandleMap(java.util.Collection<Integer> consultationIds) {
        Set<Integer> ids = consultationIds == null ? Set.of() : consultationIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) return Map.of();

        Map<Integer, ConsultationDoctorHandle> result = new java.util.HashMap<>();
        consultationDoctorHandleMapper.selectList(Wrappers.<ConsultationDoctorHandle>query()
                        .in("consultation_id", ids)
                        .orderByDesc("id"))
                .forEach(item -> result.putIfAbsent(item.getConsultationId(), item));
        return result;
    }

    private Map<Integer, ConsultationDoctorConclusion> loadDoctorConclusionMap(java.util.Collection<Integer> consultationIds) {
        Set<Integer> ids = consultationIds == null ? Set.of() : consultationIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) return Map.of();

        Map<Integer, ConsultationDoctorConclusion> result = new java.util.HashMap<>();
        consultationDoctorConclusionMapper.selectList(Wrappers.<ConsultationDoctorConclusion>query()
                        .in("consultation_id", ids)
                        .orderByDesc("id"))
                .forEach(item -> result.putIfAbsent(item.getConsultationId(), item));
        return result;
    }

    private boolean isHighRisk(ConsultationAiAuditItemVO item) {
        if (item == null) return false;
        JSONObject payload = parseStructuredPayload(item.getStructuredContent());
        if (payload != null) {
            JSONArray riskFlags = payload.getJSONArray("riskFlags");
            if (riskFlags != null && !riskFlags.isEmpty()) return true;
            if (payload.getIntValue("shouldEscalateToHuman") == 1) return true;
            if (payload.getIntValue("suggestOfflineImmediately") == 1) return true;
            String visitType = trimToNull(payload.getString("recommendedVisitType"));
            if ("emergency".equalsIgnoreCase(visitType) || "offline".equalsIgnoreCase(visitType)) return true;
        }
        String triageActionType = trimToNull(item.getTriageActionType());
        return "emergency".equalsIgnoreCase(triageActionType) || "offline".equalsIgnoreCase(triageActionType);
    }

    private boolean requiresReview(ConsultationAiAuditItemVO item) {
        if (item == null) return false;
        var doctorConclusion = item.getDoctorConclusion();
        if (doctorConclusion == null) return true;
        return Objects.equals(doctorConclusion.getIsConsistentWithAi(), 0);
    }

    private ConsultationAiAuditItemVO toAuditItem(TriageMessage message,
                                                  TriageSession session,
                                                  ConsultationRecord record,
                                                  ConsultationDoctorHandle doctorHandle,
                                                  ConsultationDoctorConclusion doctorConclusion) {
        ConsultationAiAuditItemVO vo = new ConsultationAiAuditItemVO();
        vo.setMessageId(message.getId());
        vo.setSessionId(firstNonNull(message.getSessionId(), session == null ? null : session.getId()));
        vo.setSessionNo(session == null ? null : session.getSessionNo());
        vo.setConsultationId(firstNonNull(
                session == null ? null : session.getConsultationId(),
                record == null ? null : record.getId()
        ));
        vo.setConsultationNo(record == null ? null : record.getConsultationNo());
        vo.setPatientName(firstText(
                session == null ? null : session.getPatientName(),
                record == null ? null : record.getPatientName()
        ));
        vo.setCategoryName(firstText(
                session == null ? null : session.getCategoryName(),
                record == null ? null : record.getCategoryName()
        ));
        vo.setDepartmentName(firstText(
                session == null ? null : session.getDepartmentName(),
                record == null ? null : record.getDepartmentName()
        ));
        vo.setConsultationStatus(record == null ? null : record.getStatus());
        vo.setTriageLevelName(firstText(
                session == null ? null : session.getTriageLevelName(),
                record == null ? null : record.getTriageLevelName()
        ));
        vo.setTriageActionType(firstText(
                session == null ? null : session.getTriageActionType(),
                record == null ? null : record.getTriageActionType()
        ));
        vo.setMessageType(message.getMessageType());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setStructuredContent(message.getStructuredContent());
        vo.setDoctorHandle(doctorHandle == null ? null : doctorHandle.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO.class));
        vo.setDoctorConclusion(doctorConclusion == null ? null : doctorConclusion.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO.class));
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private byte[] buildAuditItemsCsv(List<ConsultationAiAuditItemVO> items) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        appendCsvRow(builder,
                "问诊ID",
                "问诊单号",
                "会话单号",
                "患者姓名",
                "问诊分类",
                "推荐科室",
                "消息类型",
                "AI输出摘要",
                "风险标签",
                "建议方式",
                "Prompt版本",
                "来源",
                "复核状态",
                "医生处理进度",
                "AI一致性",
                "差异原因",
                "差异说明",
                "创建时间"
        );
        for (ConsultationAiAuditItemVO item : items) {
            appendCsvRow(builder,
                    item.getConsultationId() == null ? null : String.valueOf(item.getConsultationId()),
                    item.getConsultationNo(),
                    item.getSessionNo(),
                    item.getPatientName(),
                    item.getCategoryName(),
                    item.getDepartmentName(),
                    renderAuditMessageType(item.getMessageType()),
                    resolveAuditDigest(item),
                    resolveRiskFlagsText(item),
                    resolveRecommendedVisitTypeText(item),
                    resolvePromptVersion(item),
                    resolveSourceText(item),
                    resolveReviewStatusText(item),
                    resolveDoctorProgressText(item),
                    resolveAiConsistencyText(item),
                    resolveMismatchReasonText(item),
                    resolveMismatchRemark(item),
                    formatCsvDate(item.getCreateTime())
            );
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRow(StringBuilder builder, String... columns) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(escapeCsv(columns[i]));
        }
        builder.append("\r\n");
    }

    private String escapeCsv(String value) {
        String text = value == null ? "" : value;
        boolean needQuote = text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n");
        String escaped = text.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    private String resolveAuditDigest(ConsultationAiAuditItemVO item) {
        JSONObject payload = parseStructuredPayload(item == null ? null : item.getStructuredContent());
        List<String> segments = new ArrayList<>();
        if (payload != null) {
            addIfPresent(segments, payload.getString("summary"));
            addIfPresent(segments, payload.getString("reply"));
            List<String> nextQuestions = toStringList(payload.getJSONArray("nextQuestions"));
            if (!nextQuestions.isEmpty()) {
                segments.add("补充建议 " + String.join("；", nextQuestions));
            }
            addIfPresent(segments, payload.getString("doctorRecommendationReason"), "推荐依据 ");
        }
        if (segments.isEmpty()) {
            addIfPresent(segments, item == null ? null : item.getContent());
        }
        return trimToNull(String.join("；", segments));
    }

    private String resolveRiskFlagsText(ConsultationAiAuditItemVO item) {
        JSONObject payload = parseStructuredPayload(item == null ? null : item.getStructuredContent());
        if (payload == null) return null;
        List<String> riskFlags = toStringList(payload.getJSONArray("riskFlags"));
        return riskFlags.isEmpty() ? null : String.join(" / ", riskFlags);
    }

    private String resolveRecommendedVisitTypeText(ConsultationAiAuditItemVO item) {
        JSONObject payload = parseStructuredPayload(item == null ? null : item.getStructuredContent());
        String visitType = payload == null ? null : trimToNull(payload.getString("recommendedVisitType"));
        if (visitType == null) {
            visitType = trimToNull(item == null ? null : item.getTriageActionType());
        }
        return renderVisitType(visitType);
    }

    private String resolvePromptVersion(ConsultationAiAuditItemVO item) {
        JSONObject payload = parseStructuredPayload(item == null ? null : item.getStructuredContent());
        return payload == null ? null : trimToNull(payload.getString("promptVersion"));
    }

    private String resolveSourceText(ConsultationAiAuditItemVO item) {
        JSONObject payload = parseStructuredPayload(item == null ? null : item.getStructuredContent());
        return renderSource(payload == null ? null : payload.getString("source"));
    }

    private String resolveReviewStatusText(ConsultationAiAuditItemVO item) {
        if (item == null) return null;
        if (item.getDoctorConclusion() == null && item.getDoctorHandle() == null) return "待医生接手";
        if (item.getDoctorConclusion() == null) return "待形成结论";
        if (Objects.equals(item.getDoctorConclusion().getIsConsistentWithAi(), 0)) return "医生判定有差异";
        return "已完成复核";
    }

    private String resolveDoctorProgressText(ConsultationAiAuditItemVO item) {
        if (item == null) return null;
        var conclusion = item.getDoctorConclusion();
        if (conclusion != null) {
            String doctorName = trimToNull(conclusion.getDoctorName());
            String updateTime = formatCsvDate(firstDate(conclusion.getUpdateTime(), conclusion.getCreateTime()));
            return joinDisplayText(
                    doctorName == null ? "已形成医生最终结论" : doctorName + " 已形成最终结论",
                    updateTime == null ? null : "更新时间 " + updateTime
            );
        }
        var handle = item.getDoctorHandle();
        if (handle == null) return "当前还没有医生接手";
        String doctorName = trimToNull(handle.getDoctorName());
        String status = renderDoctorHandleStatus(handle.getStatus());
        String updateTime = formatCsvDate(firstDate(handle.getUpdateTime(), handle.getReceiveTime()));
        return joinDisplayText(
                doctorName == null ? status : doctorName + " " + status,
                updateTime == null ? null : "最近时间 " + updateTime
        );
    }

    private String resolveAiConsistencyText(ConsultationAiAuditItemVO item) {
        if (item == null || item.getDoctorConclusion() == null) return null;
        Integer value = item.getDoctorConclusion().getIsConsistentWithAi();
        if (Objects.equals(value, 1)) return "医生标记与 AI 一致";
        if (Objects.equals(value, 0)) return "医生标记与 AI 不一致";
        return "医生未标记";
    }

    private String resolveMismatchReasonText(ConsultationAiAuditItemVO item) {
        if (item == null || item.getDoctorConclusion() == null) return null;
        List<String> reasonCodes = ConsultationAiMismatchReasonUtils.parseCodes(item.getDoctorConclusion().getAiMismatchReasonsJson());
        if (reasonCodes.isEmpty() && trimToNull(item.getDoctorConclusion().getAiMismatchRemark()) != null) {
            return ConsultationAiMismatchReasonUtils.labelOf("other");
        }
        if (reasonCodes.isEmpty()) return null;
        return reasonCodes.stream()
                .map(ConsultationAiMismatchReasonUtils::labelOf)
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
    }

    private String resolveMismatchRemark(ConsultationAiAuditItemVO item) {
        if (item == null || item.getDoctorConclusion() == null) return null;
        return trimToNull(item.getDoctorConclusion().getAiMismatchRemark());
    }

    private List<String> toStringList(JSONArray array) {
        if (array == null || array.isEmpty()) return List.of();
        return array.stream()
                .map(item -> trimToNull(item == null ? null : String.valueOf(item)))
                .filter(Objects::nonNull)
                .distinct()
                .limit(6)
                .toList();
    }

    private void addIfPresent(List<String> segments, String value) {
        addIfPresent(segments, value, null);
    }

    private void addIfPresent(List<String> segments, String value, String prefix) {
        String text = trimToNull(value);
        if (text == null) return;
        segments.add(prefix == null ? text : prefix + text);
    }

    private String joinDisplayText(String first, String second) {
        String left = trimToNull(first);
        String right = trimToNull(second);
        if (left == null) return right;
        if (right == null) return left;
        return left + "；" + right;
    }

    private String renderAuditMessageType(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value)) {
            case "ai_triage_summary" -> "AI 导诊总结";
            case "ai_followup_questions" -> "AI 追问建议";
            case "ai_chat_reply" -> "AI 对话回复";
            default -> trimToNull(value);
        };
    }

    private String renderVisitType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        return switch (normalized.toLowerCase()) {
            case "emergency" -> "立即急诊";
            case "offline" -> "尽快线下就医";
            case "followup" -> "复诊随访";
            case "online" -> "线上继续沟通";
            default -> normalized;
        };
    }

    private String renderSource(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        return "deepseek".equalsIgnoreCase(normalized) ? "DeepSeek" : normalized;
    }

    private String renderDoctorHandleStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return "已接手";
        return switch (normalized) {
            case "completed" -> "处理完成";
            case "processing" -> "处理中";
            case "received" -> "已接诊";
            default -> normalized;
        };
    }

    private List<AdminDoctorMessageAiUsageSceneVO> buildDoctorMessageSceneBreakdown(List<DoctorMessageAiLog> logs) {
        return List.of("opening", "clarify", "check_result", "follow_up").stream()
                .map(sceneType -> {
                    List<DoctorMessageAiLog> sceneLogs = logs.stream()
                            .filter(item -> sceneType.equalsIgnoreCase(trimToNull(item.getSceneType())))
                            .toList();
                    AdminDoctorMessageAiUsageSceneVO item = new AdminDoctorMessageAiUsageSceneVO();
                    item.setSceneType(sceneType);
                    item.setSceneLabel(renderDoctorMessageSceneLabel(sceneType));
                    item.setGeneratedCount(sceneLogs.size());
                    item.setAppliedCount((int) sceneLogs.stream().filter(log -> defaultInt(log.getApplyCount()) > 0).count());
                    item.setSentCount((int) sceneLogs.stream().filter(log -> Objects.equals(log.getSentStatus(), 1)).count());
                    return item;
                })
                .filter(item -> defaultInt(item.getGeneratedCount()) > 0)
                .toList();
    }

    private List<AdminDoctorFormAiUsageSceneVO> buildDoctorFormSceneBreakdown(List<DoctorFormAiLog> logs) {
        return List.of("handle", "follow_up").stream()
                .map(sceneType -> {
                    List<DoctorFormAiLog> sceneLogs = logs.stream()
                            .filter(item -> sceneType.equalsIgnoreCase(trimToNull(item.getSceneType())))
                            .toList();
                    AdminDoctorFormAiUsageSceneVO item = new AdminDoctorFormAiUsageSceneVO();
                    item.setSceneType(sceneType);
                    item.setSceneLabel(renderDoctorFormSceneLabel(sceneType));
                    item.setGeneratedCount(sceneLogs.size());
                    item.setFullGeneratedCount((int) sceneLogs.stream().filter(log -> trimToNull(log.getRegenerateField()) == null).count());
                    item.setFieldRegenerateCount((int) sceneLogs.stream().filter(log -> trimToNull(log.getRegenerateField()) != null).count());
                    item.setAppliedCount((int) sceneLogs.stream().filter(log -> defaultInt(log.getApplyCount()) > 0).count());
                    item.setPureAiAppliedCount((int) sceneLogs.stream().filter(log -> defaultInt(log.getApplyCount()) > 0
                            && !Objects.equals(log.getTemplateUsed(), 1)).count());
                    item.setTemplateUsedCount((int) sceneLogs.stream().filter(log -> defaultInt(log.getApplyCount()) > 0
                            && Objects.equals(log.getTemplateUsed(), 1)).count());
                    item.setSavedCount((int) sceneLogs.stream().filter(log -> Objects.equals(log.getSavedStatus(), 1)).count());
                    return item;
                })
                .filter(item -> defaultInt(item.getGeneratedCount()) > 0)
                .toList();
    }

    private List<AdminDoctorFormAiUsagePromptVO> buildDoctorFormPromptBreakdown(List<DoctorFormAiLog> logs) {
        Map<String, List<DoctorFormAiLog>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> firstText(trimToNull(log.getPromptVersion()), "unknown"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        if (groupedLogs.isEmpty()) return List.of();

        return groupedLogs.entrySet().stream()
                .map(entry -> {
                    List<DoctorFormAiLog> group = entry.getValue();
                    AdminDoctorFormAiUsagePromptVO item = new AdminDoctorFormAiUsagePromptVO();
                    item.setPromptVersion(entry.getKey());
                    item.setGeneratedCount(group.size());
                    item.setFullGeneratedCount((int) group.stream().filter(log -> trimToNull(log.getRegenerateField()) == null).count());
                    item.setFieldRegenerateCount((int) group.stream().filter(log -> trimToNull(log.getRegenerateField()) != null).count());
                    item.setContextRewriteCount((int) group.stream().filter(log -> Objects.equals(log.getDraftContextUsed(), 1)).count());
                    item.setAppliedCount((int) group.stream().filter(log -> defaultInt(log.getApplyCount()) > 0).count());
                    item.setSavedCount((int) group.stream().filter(log -> Objects.equals(log.getSavedStatus(), 1)).count());
                    item.setApplyRate(resolveRate(item.getAppliedCount(), item.getGeneratedCount()));
                    item.setSaveRate(resolveRate(item.getSavedCount(), item.getGeneratedCount()));
                    return item;
                })
                .sorted((left, right) -> {
                    int compare = Integer.compare(defaultInt(right.getGeneratedCount()), defaultInt(left.getGeneratedCount()));
                    if (compare != 0) return compare;
                    compare = Integer.compare(defaultInt(right.getContextRewriteCount()), defaultInt(left.getContextRewriteCount()));
                    if (compare != 0) return compare;
                    compare = Integer.compare(defaultInt(right.getSavedCount()), defaultInt(left.getSavedCount()));
                    if (compare != 0) return compare;
                    return String.valueOf(left.getPromptVersion()).compareTo(String.valueOf(right.getPromptVersion()));
                })
                .toList();
    }

    private List<AdminDoctorFormAiUsageFieldVO> buildDoctorFormFieldBreakdown(List<DoctorFormAiLog> logs) {
        return List.of(
                        "doctor_summary",
                        "medical_advice",
                        "follow_up_plan",
                        "patient_instruction",
                        "followup_summary",
                        "followup_advice",
                        "followup_next_step"
                ).stream()
                .map(fieldKey -> {
                    AdminDoctorFormAiUsageFieldVO item = new AdminDoctorFormAiUsageFieldVO();
                    item.setFieldKey(fieldKey);
                    item.setFieldLabel(renderDoctorFormFieldLabel(fieldKey));
                    item.setSceneType(resolveDoctorFormFieldSceneType(fieldKey));
                    item.setSceneLabel(renderDoctorFormSceneLabel(item.getSceneType()));
                    int relatedCount = (int) logs.stream()
                            .filter(log -> matchesDoctorFormFieldUsage(log, fieldKey))
                            .count();
                    item.setRelatedCount(relatedCount);
                    item.setRegenerateCount((int) logs.stream()
                            .filter(log -> fieldKey.equalsIgnoreCase(trimToNull(log.getRegenerateField())))
                            .count());
                    item.setTemplateComposeCount((int) logs.stream()
                            .filter(log -> Objects.equals(log.getTemplateUsed(), 1))
                            .filter(log -> fieldKey.equalsIgnoreCase(resolveDoctorFormFieldKeyFromTemplateScene(log.getTemplateSceneType())))
                            .count());
                    item.setContextRewriteCount((int) logs.stream()
                            .filter(log -> Objects.equals(log.getDraftContextUsed(), 1))
                            .filter(log -> matchesDoctorFormFieldUsage(log, fieldKey))
                            .count());
                    item.setAppliedCount((int) logs.stream()
                            .filter(log -> defaultInt(log.getApplyCount()) > 0)
                            .filter(log -> matchesDoctorFormFieldUsage(log, fieldKey))
                            .count());
                    item.setSavedCount((int) logs.stream()
                            .filter(log -> Objects.equals(log.getSavedStatus(), 1))
                            .filter(log -> matchesDoctorFormFieldUsage(log, fieldKey))
                            .count());
                    item.setContextRewriteSavedCount((int) logs.stream()
                            .filter(log -> Objects.equals(log.getDraftContextUsed(), 1))
                            .filter(log -> Objects.equals(log.getSavedStatus(), 1))
                            .filter(log -> matchesDoctorFormFieldUsage(log, fieldKey))
                            .count());
                    item.setApplyRate(resolveRate(item.getAppliedCount(), relatedCount));
                    item.setSaveRate(resolveRate(item.getSavedCount(), relatedCount));
                    item.setContextRewriteSaveRate(resolveRate(item.getContextRewriteSavedCount(), item.getContextRewriteCount()));
                    return item;
                })
                .filter(item -> defaultInt(item.getRelatedCount()) > 0
                        || defaultInt(item.getRegenerateCount()) > 0
                        || defaultInt(item.getTemplateComposeCount()) > 0
                        || defaultInt(item.getContextRewriteCount()) > 0
                        || defaultInt(item.getAppliedCount()) > 0
                        || defaultInt(item.getSavedCount()) > 0)
                .sorted((left, right) -> {
                    int compare = Integer.compare(defaultInt(right.getRelatedCount()), defaultInt(left.getRelatedCount()));
                    if (compare != 0) return compare;
                    compare = Integer.compare(defaultInt(right.getRegenerateCount()), defaultInt(left.getRegenerateCount()));
                    if (compare != 0) return compare;
                    compare = Integer.compare(defaultInt(right.getTemplateComposeCount()), defaultInt(left.getTemplateComposeCount()));
                    if (compare != 0) return compare;
                    compare = Integer.compare(defaultInt(right.getSavedCount()), defaultInt(left.getSavedCount()));
                    if (compare != 0) return compare;
                    return String.valueOf(left.getFieldLabel()).compareTo(String.valueOf(right.getFieldLabel()));
                })
                .toList();
    }

    private List<AdminDoctorFormAiUsageTemplateVO> buildDoctorFormTemplateBreakdown(List<DoctorFormAiLog> logs) {
        Map<String, List<DoctorFormAiLog>> groupedLogs = logs.stream()
                .filter(log -> Objects.equals(log.getTemplateUsed(), 1))
                .filter(log -> resolveDoctorFormFieldKeyFromTemplateScene(log.getTemplateSceneType()) != null)
                .collect(Collectors.groupingBy(
                        this::buildDoctorFormTemplateBreakdownKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        if (groupedLogs.isEmpty()) return List.of();

        return groupedLogs.values().stream()
                .map(group -> {
                    DoctorFormAiLog firstLog = group.get(0);
                    String fieldKey = resolveDoctorFormFieldKeyFromTemplateScene(firstLog.getTemplateSceneType());
                    if (fieldKey == null) return null;

                    AdminDoctorFormAiUsageTemplateVO item = new AdminDoctorFormAiUsageTemplateVO();
                    item.setFieldKey(fieldKey);
                    item.setFieldLabel(renderDoctorFormFieldLabel(fieldKey));
                    item.setSceneType(resolveDoctorFormFieldSceneType(fieldKey));
                    item.setSceneLabel(renderDoctorFormSceneLabel(item.getSceneType()));
                    item.setTemplateSceneType(trimToNull(firstLog.getTemplateSceneType()));
                    item.setTemplateId(firstLog.getTemplateId());
                    item.setTemplateTitle(trimToNull(firstLog.getTemplateTitle()));
                    item.setTemplateLabel(resolveDoctorFormTemplateLabel(firstLog));
                    item.setComposeCount(group.size());
                    item.setContextRewriteCount((int) group.stream().filter(log -> Objects.equals(log.getDraftContextUsed(), 1)).count());
                    item.setAppliedCount((int) group.stream().filter(log -> defaultInt(log.getApplyCount()) > 0).count());
                    item.setSavedCount((int) group.stream().filter(log -> Objects.equals(log.getSavedStatus(), 1)).count());
                    item.setContextRewriteSavedCount((int) group.stream()
                            .filter(log -> Objects.equals(log.getDraftContextUsed(), 1))
                            .filter(log -> Objects.equals(log.getSavedStatus(), 1))
                            .count());
                    item.setApplyRate(resolveRate(item.getAppliedCount(), item.getComposeCount()));
                    item.setSaveRate(resolveRate(item.getSavedCount(), item.getComposeCount()));
                    item.setContextRewriteSaveRate(resolveRate(item.getContextRewriteSavedCount(), item.getContextRewriteCount()));
                    return item;
                })
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    int compare = Integer.compare(defaultInt(right.getComposeCount()), defaultInt(left.getComposeCount()));
                    if (compare != 0) return compare;
                    compare = Integer.compare(defaultInt(right.getSavedCount()), defaultInt(left.getSavedCount()));
                    if (compare != 0) return compare;
                    compare = Double.compare(right.getSaveRate() == null ? 0D : right.getSaveRate(),
                            left.getSaveRate() == null ? 0D : left.getSaveRate());
                    if (compare != 0) return compare;
                    compare = String.valueOf(left.getFieldLabel()).compareTo(String.valueOf(right.getFieldLabel()));
                    if (compare != 0) return compare;
                    return String.valueOf(left.getTemplateLabel()).compareTo(String.valueOf(right.getTemplateLabel()));
                })
                .toList();
    }

    private AdminDoctorMessageAiUsageItemVO toDoctorMessageUsageItem(DoctorMessageAiLog log) {
        AdminDoctorMessageAiUsageItemVO item = new AdminDoctorMessageAiUsageItemVO();
        item.setLogId(log.getId());
        item.setConsultationId(log.getConsultationId());
        item.setConsultationNo(log.getConsultationNo());
        item.setPatientName(log.getPatientName());
        item.setCategoryName(log.getCategoryName());
        item.setDoctorId(log.getDoctorId());
        item.setDoctorName(log.getDoctorName());
        item.setDepartmentName(log.getDepartmentName());
        item.setSceneType(trimToNull(log.getSceneType()));
        item.setSource(renderSource(firstText(trimToNull(log.getSource()), Objects.equals(log.getFallback(), 1) ? "fallback" : "deepseek")));
        item.setFallback(log.getFallback());
        item.setApplyCount(defaultInt(log.getApplyCount()));
        item.setLastApplyMode(trimToNull(log.getLastApplyMode()));
        item.setTemplateUsed(log.getTemplateUsed());
        item.setTemplateTitle(trimToNull(log.getTemplateTitle()));
        item.setSentStatus(log.getSentStatus());
        item.setDraftSummary(trimToNull(log.getDraftSummary()));
        item.setDraftContent(trimToNull(log.getDraftContent()));
        item.setSentContentPreview(trimToNull(log.getSentContentPreview()));
        item.setGeneratedTime(log.getGeneratedTime());
        item.setLastApplyTime(log.getLastApplyTime());
        item.setSentTime(log.getSentTime());
        return item;
    }

    private AdminDoctorFormAiUsageItemVO toDoctorFormUsageItem(DoctorFormAiLog log) {
        AdminDoctorFormAiUsageItemVO item = new AdminDoctorFormAiUsageItemVO();
        item.setLogId(log.getId());
        item.setConsultationId(log.getConsultationId());
        item.setConsultationNo(log.getConsultationNo());
        item.setPatientName(log.getPatientName());
        item.setCategoryName(log.getCategoryName());
        item.setDoctorId(log.getDoctorId());
        item.setDoctorName(log.getDoctorName());
        item.setDepartmentName(log.getDepartmentName());
        item.setSceneType(trimToNull(log.getSceneType()));
        item.setRegenerateField(trimToNull(log.getRegenerateField()));
        item.setSource(renderSource(firstText(trimToNull(log.getSource()), Objects.equals(log.getFallback(), 1) ? "fallback" : "deepseek")));
        item.setPromptVersion(trimToNull(log.getPromptVersion()));
        item.setFallback(log.getFallback());
        item.setDraftContextUsed(log.getDraftContextUsed());
        item.setRewriteRequirement(trimToNull(log.getRewriteRequirement()));
        item.setApplyCount(defaultInt(log.getApplyCount()));
        item.setLastApplyMode(trimToNull(log.getLastApplyMode()));
        item.setLastApplyTarget(trimToNull(log.getLastApplyTarget()));
        item.setTemplateUsed(log.getTemplateUsed());
        item.setTemplateTitle(trimToNull(log.getTemplateTitle()));
        item.setSavedStatus(log.getSavedStatus());
        item.setSavedTarget(trimToNull(log.getSavedTarget()));
        item.setDraftSummary(trimToNull(log.getDraftSummary()));
        item.setDraftPreview(trimToNull(log.getDraftPreview()));
        item.setSavedPreview(trimToNull(log.getSavedPreview()));
        item.setGeneratedTime(log.getGeneratedTime());
        item.setLastApplyTime(log.getLastApplyTime());
        item.setSavedTime(log.getSavedTime());
        return item;
    }

    private boolean matchesDoctorFormFieldUsage(DoctorFormAiLog log, String fieldKey) {
        if (log == null || !StringUtils.hasText(fieldKey)) return false;
        String normalizedFieldKey = fieldKey.trim().toLowerCase();
        String regenerateField = trimToNull(log.getRegenerateField());
        if (normalizedFieldKey.equalsIgnoreCase(regenerateField)) {
            return true;
        }
        return normalizedFieldKey.equalsIgnoreCase(resolveDoctorFormFieldKeyFromTemplateScene(log.getTemplateSceneType()));
    }

    private String resolveDoctorFormFieldKeyFromTemplateScene(String templateSceneType) {
        String normalized = trimToNull(templateSceneType);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return switch (normalized) {
            case "handle_summary" -> "doctor_summary";
            case "medical_advice", "follow_up_plan", "patient_instruction",
                    "followup_summary", "followup_advice", "followup_next_step" -> normalized;
            default -> null;
        };
    }

    private String buildDoctorFormTemplateBreakdownKey(DoctorFormAiLog log) {
        String fieldKey = resolveDoctorFormFieldKeyFromTemplateScene(log == null ? null : log.getTemplateSceneType());
        String sceneType = resolveDoctorFormFieldSceneType(fieldKey);
        String templateId = log == null || log.getTemplateId() == null ? "" : String.valueOf(log.getTemplateId());
        String templateLabel = resolveDoctorFormTemplateLabel(log);
        return String.join("|",
                firstText(fieldKey, ""),
                firstText(sceneType, ""),
                templateId,
                firstText(templateLabel, ""));
    }

    private String resolveDoctorFormTemplateLabel(DoctorFormAiLog log) {
        if (log == null) return "未命名模板";
        return firstText(
                trimToNull(log.getTemplateTitle()),
                firstText(renderDoctorFormTemplateSceneLabel(log.getTemplateSceneType()), "未命名模板")
        );
    }

    private String resolveDoctorFormFieldSceneType(String fieldKey) {
        String normalized = trimToNull(fieldKey);
        if (normalized == null) return "handle";
        normalized = normalized.toLowerCase();
        return List.of("followup_summary", "followup_advice", "followup_next_step").contains(normalized)
                ? "follow_up"
                : "handle";
    }

    private String renderDoctorFormFieldLabel(String fieldKey) {
        String normalized = trimToNull(fieldKey);
        if (normalized == null) return "表单字段";
        return switch (normalized.toLowerCase()) {
            case "doctor_summary" -> "判断摘要";
            case "medical_advice" -> "处理建议";
            case "follow_up_plan" -> "随访安排";
            case "patient_instruction" -> "患者指导";
            case "followup_summary" -> "随访摘要";
            case "followup_advice" -> "随访建议";
            case "followup_next_step" -> "下一步安排";
            default -> normalized;
        };
    }

    private String renderDoctorFormTemplateSceneLabel(String templateSceneType) {
        String normalized = trimToNull(templateSceneType);
        if (normalized == null) return "未命名模板";
        return switch (normalized.toLowerCase()) {
            case "handle_summary" -> "处理摘要模板";
            case "medical_advice" -> "处理建议模板";
            case "follow_up_plan" -> "随访安排模板";
            case "patient_instruction" -> "患者指导模板";
            case "followup_summary" -> "随访摘要模板";
            case "followup_advice" -> "随访建议模板";
            case "followup_next_step" -> "下一步安排模板";
            default -> normalized;
        };
    }

    private String renderDoctorMessageSceneLabel(String sceneType) {
        String normalized = trimToNull(sceneType);
        if (normalized == null) return "沟通草稿";
        return switch (normalized.toLowerCase()) {
            case "opening" -> "首次接诊";
            case "clarify" -> "补充追问";
            case "check_result" -> "结果解读";
            case "follow_up" -> "复诊随访";
            default -> normalized;
        };
    }

    private String renderDoctorFormSceneLabel(String sceneType) {
        String normalized = trimToNull(sceneType);
        if (normalized == null) return "表单草稿";
        return switch (normalized.toLowerCase()) {
            case "handle" -> "医生处理";
            case "follow_up" -> "随访记录";
            default -> normalized;
        };
    }

    private Double resolveRate(Integer value, Integer total) {
        int numerator = defaultInt(value);
        int denominator = defaultInt(total);
        if (denominator <= 0 || numerator <= 0) return 0D;
        return Math.round((numerator * 10000D) / denominator) / 100D;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        String sourceText = trimToNull(source);
        String keywordText = trimToNull(keyword);
        if (sourceText == null || keywordText == null) return false;
        return sourceText.toLowerCase().contains(keywordText.toLowerCase());
    }

    private String formatCsvDate(Date value) {
        if (value == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    private Date firstDate(Date first, Date second) {
        return first != null ? first : second;
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(environment.getProperty("spring.ai.deepseek.api-key"))
                || StringUtils.hasText(environment.getProperty("spring.ai.deepseek.chat.api-key"));
    }

    private JSONObject parseStructuredPayload(String structuredContent) {
        if (!StringUtils.hasText(structuredContent)) return null;
        try {
            return JSON.parseObject(structuredContent);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer firstNonNull(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String resolveRuntimeStatus(boolean triageEnabled,
                                        boolean providerEnabled,
                                        boolean apiKeyConfigured,
                                        boolean modelBeanReady,
                                        boolean runtimeAvailable) {
        if (runtimeAvailable) {
            return "AI 导诊已就绪，当前可用于首轮导诊增强与患者侧继续追问。";
        }
        if (!triageEnabled) {
            return "AI 导诊总开关当前处于关闭状态，系统会继续使用规则分诊兜底。";
        }
        if (!providerEnabled) {
            return "DeepSeek Chat 模型接入尚未启用，当前不会发起大模型调用。";
        }
        if (!apiKeyConfigured) {
            return "当前尚未检测到 DeepSeek API Key，AI 导诊仍会自动回退为规则模式。";
        }
        if (!modelBeanReady) {
            return "DeepSeek ChatModel 尚未成功装配，请检查 Spring AI 依赖与模型配置。";
        }
        return "AI 导诊当前暂不可用，请检查模型配置与启动日志。";
    }

    private List<String> buildWarnings(boolean triageEnabled,
                                       boolean providerEnabled,
                                       boolean apiKeyConfigured,
                                       boolean modelBeanReady) {
        List<String> warnings = new ArrayList<>();
        if (!triageEnabled) {
            warnings.add("consultation.ai.triage.enabled 已关闭，患者侧与问诊提交后的 AI 导诊增强不会执行。");
        }
        if (!providerEnabled) {
            warnings.add("spring.ai.deepseek.chat.enabled 未开启，DeepSeek Chat 模型当前不会参与运行。");
        }
        if (!apiKeyConfigured) {
            warnings.add("尚未配置 DEEPSEEK_API_KEY，当前环境无法真正调用 DeepSeek。");
        }
        if (!modelBeanReady) {
            warnings.add("Spring AI 未发现可用的 DeepSeekChatModel Bean，请检查模型依赖与配置项是否生效。");
        }
        if (!StringUtils.hasText(properties.getPromptVersion())) {
            warnings.add("当前未配置 promptVersion，建议为导诊输出保留清晰的 Prompt 版本标识。");
        }
        if (properties.getDoctorCandidateLimit() <= 0) {
            warnings.add("doctorCandidateLimit 当前小于等于 0，推荐医生候选上限配置异常。");
        }
        return warnings;
    }
}
