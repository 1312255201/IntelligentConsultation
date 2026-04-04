package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageAdvice;
import cn.gugufish.ai.AiTriageContext;
import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.TriageMessageMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.AiTriageService;
import cn.gugufish.service.ConsultationAiEnrichmentService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConsultationAiEnrichmentServiceImpl implements ConsultationAiEnrichmentService {

    private final ConsultationRecordMapper consultationRecordMapper;
    private final ConsultationRecordAnswerMapper consultationRecordAnswerMapper;
    private final TriageSessionMapper triageSessionMapper;
    private final TriageMessageMapper triageMessageMapper;
    private final TriageResultMapper triageResultMapper;
    private final AiTriageService aiTriageService;
    private final AiTriageProperties properties;

    public ConsultationAiEnrichmentServiceImpl(ConsultationRecordMapper consultationRecordMapper,
                                               ConsultationRecordAnswerMapper consultationRecordAnswerMapper,
                                               TriageSessionMapper triageSessionMapper,
                                               TriageMessageMapper triageMessageMapper,
                                               TriageResultMapper triageResultMapper,
                                               AiTriageService aiTriageService,
                                               AiTriageProperties properties) {
        this.consultationRecordMapper = consultationRecordMapper;
        this.consultationRecordAnswerMapper = consultationRecordAnswerMapper;
        this.triageSessionMapper = triageSessionMapper;
        this.triageMessageMapper = triageMessageMapper;
        this.triageResultMapper = triageResultMapper;
        this.aiTriageService = aiTriageService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void enrichInitialTriage(int consultationId) {
        if (!aiTriageService.isAvailable()) return;

        ConsultationRecord record = consultationRecordMapper.selectById(consultationId);
        if (record == null) return;

        TriageSession session = triageSessionMapper.selectOne(Wrappers.<TriageSession>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
        if (session == null) return;

        boolean aiSummaryExists = triageMessageMapper.exists(Wrappers.<TriageMessage>query()
                .eq("session_id", session.getId())
                .eq("message_type", "ai_triage_summary"));
        if (aiSummaryExists) return;

        List<ConsultationRecordAnswer> answers = consultationRecordAnswerMapper.selectList(Wrappers.<ConsultationRecordAnswer>query()
                .eq("consultation_id", consultationId)
                .orderByAsc("sort")
                .orderByAsc("id"));

        TriageResult triageResult = triageResultMapper.selectOne(Wrappers.<TriageResult>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("is_final")
                .orderByDesc("id")
                .last("limit 1"));

        List<ConsultationRecommendDoctorVO> doctorCandidates = parseDoctorCandidates(triageResult);
        AiTriageAdvice advice = aiTriageService.generateInitialAdvice(AiTriageContext.builder()
                .record(record)
                .answers(answers)
                .doctorCandidates(doctorCandidates)
                .build());
        if (advice == null) return;

        List<TriageMessage> messages = buildAiMessages(session.getId(), consultationId, advice);
        if (messages.isEmpty()) return;

        for (TriageMessage message : messages) {
            triageMessageMapper.insert(message);
        }

        session.setMessageCount(defaultInt(session.getMessageCount()) + messages.size());
        session.setUpdateTime(new Date());
        triageSessionMapper.updateById(session);

        log.info("AI triage enrichment completed for consultation {}, inserted {} messages",
                consultationId,
                messages.size());
    }

    private List<ConsultationRecommendDoctorVO> parseDoctorCandidates(TriageResult triageResult) {
        if (triageResult == null || !StringUtils.hasText(triageResult.getDoctorCandidatesJson())) return List.of();
        try {
            List<ConsultationRecommendDoctorVO> items = JSON.parseArray(
                    triageResult.getDoctorCandidatesJson(),
                    ConsultationRecommendDoctorVO.class
            );
            return items == null ? List.of() : items.stream()
                    .limit(Math.max(properties.getDoctorCandidateLimit(), 1))
                    .toList();
        } catch (Exception exception) {
            log.warn("Failed to parse doctor candidates for AI triage: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<TriageMessage> buildAiMessages(Integer sessionId,
                                                int consultationId,
                                                AiTriageAdvice advice) {
        Date now = new Date();
        int startSort = nextSort(sessionId);
        List<TriageMessage> messages = new ArrayList<>();

        String summaryContent = buildSummaryContent(advice);
        if (StringUtils.hasText(summaryContent)) {
            messages.add(new TriageMessage(
                    null,
                    sessionId,
                    "assistant",
                    "ai_triage_summary",
                    "AI 导诊建议",
                    summaryContent,
                    buildSummaryStructuredContent(consultationId, advice),
                    startSort,
                    now
            ));
            startSort += 10;
        }

        String nextQuestionContent = buildNextQuestionContent(advice.getNextQuestions());
        if (StringUtils.hasText(nextQuestionContent)) {
            messages.add(new TriageMessage(
                    null,
                    sessionId,
                    "assistant",
                    "ai_followup_questions",
                    "AI 建议补充信息",
                    nextQuestionContent,
                    buildQuestionStructuredContent(consultationId, advice),
                    startSort,
                    now
            ));
        }
        return messages;
    }

    private int nextSort(Integer sessionId) {
        TriageMessage lastMessage = triageMessageMapper.selectOne(Wrappers.<TriageMessage>query()
                .eq("session_id", sessionId)
                .orderByDesc("sort")
                .orderByDesc("id")
                .last("limit 1"));
        int current = lastMessage == null || lastMessage.getSort() == null ? 0 : lastMessage.getSort();
        return current + 10;
    }

    private String buildSummaryContent(AiTriageAdvice advice) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(advice.getSummary())) {
            segments.add("导诊总结：" + advice.getSummary().trim());
        }
        if (!isEmptyList(advice.getRiskFlags())) {
            segments.add("风险提示：" + String.join("、", advice.getRiskFlags()));
        }
        if (StringUtils.hasText(advice.getRecommendedDepartmentName())) {
            segments.add("建议科室：" + advice.getRecommendedDepartmentName().trim());
        }
        if (StringUtils.hasText(advice.getRecommendedVisitType())) {
            segments.add("建议方式：" + visitTypeLabel(advice.getRecommendedVisitType()));
        }

        String doctorText = buildDoctorText(advice);
        if (StringUtils.hasText(doctorText)) {
            segments.add("推荐医生：" + doctorText);
        }
        if (StringUtils.hasText(advice.getDoctorRecommendationReason())) {
            segments.add("推荐依据：" + advice.getDoctorRecommendationReason().trim());
        }
        if (advice.getSuggestOfflineImmediately() != null && advice.getSuggestOfflineImmediately() == 1) {
            segments.add("提醒：当前情况更建议尽快线下就医。");
        }
        if (advice.getShouldEscalateToHuman() != null && advice.getShouldEscalateToHuman() == 1) {
            segments.add("提醒：建议尽快由医生进一步接管。");
        }
        return segments.isEmpty() ? null : String.join("；", segments);
    }

    private String buildNextQuestionContent(List<String> nextQuestions) {
        if (isEmptyList(nextQuestions)) return null;
        return nextQuestions.stream()
                .filter(StringUtils::hasText)
                .limit(3)
                .map(item -> "• " + item.trim())
                .collect(Collectors.joining("\n"));
    }

    private String buildDoctorText(AiTriageAdvice advice) {
        List<String> doctorNames = advice.getRecommendedDoctorNames() == null ? List.of() : advice.getRecommendedDoctorNames();
        if (!doctorNames.isEmpty()) return String.join("、", doctorNames);
        if (advice.getRecommendedDoctorIds() == null || advice.getRecommendedDoctorIds().isEmpty()) return null;
        return advice.getRecommendedDoctorIds().stream()
                .map(item -> "ID=" + item)
                .collect(Collectors.joining("、"));
    }

    private String buildSummaryStructuredContent(int consultationId, AiTriageAdvice advice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("consultationId", consultationId);
        payload.put("promptVersion", properties.getPromptVersion());
        payload.put("source", "deepseek");
        payload.put("summary", advice.getSummary());
        payload.put("riskFlags", advice.getRiskFlags());
        payload.put("recommendedDepartmentName", advice.getRecommendedDepartmentName());
        payload.put("recommendedVisitType", advice.getRecommendedVisitType());
        payload.put("recommendedDoctorIds", advice.getRecommendedDoctorIds());
        payload.put("recommendedDoctorNames", advice.getRecommendedDoctorNames());
        payload.put("doctorRecommendationReason", advice.getDoctorRecommendationReason());
        payload.put("shouldEscalateToHuman", advice.getShouldEscalateToHuman());
        payload.put("suggestOfflineImmediately", advice.getSuggestOfflineImmediately());
        payload.put("confidenceScore", advice.getConfidenceScore());
        return JSON.toJSONString(payload);
    }

    private String buildQuestionStructuredContent(int consultationId, AiTriageAdvice advice) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("consultationId", consultationId);
        payload.put("promptVersion", properties.getPromptVersion());
        payload.put("source", "deepseek");
        payload.put("nextQuestions", advice.getNextQuestions());
        return JSON.toJSONString(payload);
    }

    private String visitTypeLabel(String visitType) {
        return switch (visitType == null ? "" : visitType.toLowerCase()) {
            case "emergency" -> "立即急诊";
            case "offline" -> "尽快线下就医";
            case "followup" -> "复诊随访";
            case "online" -> "线上继续沟通";
            default -> visitType;
        };
    }

    private boolean isEmptyList(List<?> values) {
        return values == null || values.isEmpty();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
