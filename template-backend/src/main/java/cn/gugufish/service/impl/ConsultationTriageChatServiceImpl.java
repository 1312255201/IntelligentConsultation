package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageAdvice;
import cn.gugufish.ai.AiTriageContext;
import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.request.ConsultationTriageMessageSendVO;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.TriageMessageMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.AiTriageService;
import cn.gugufish.service.ConsultationTriageChatService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
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

@Service
public class ConsultationTriageChatServiceImpl implements ConsultationTriageChatService {

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationRecordAnswerMapper consultationRecordAnswerMapper;

    @Resource
    TriageSessionMapper triageSessionMapper;

    @Resource
    TriageMessageMapper triageMessageMapper;

    @Resource
    TriageResultMapper triageResultMapper;

    @Resource
    AiTriageService aiTriageService;

    @Resource
    AiTriageProperties properties;

    @Override
    @Transactional
    public String sendUserMessage(int accountId, ConsultationTriageMessageSendVO vo) {
        if (!aiTriageService.isAvailable()) return "AI 导诊暂未启用，请稍后再试";

        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", vo.getRecordId())
                .eq("account_id", accountId));
        if (record == null) return "问诊记录不存在或暂无发送权限";

        TriageSession session = triageSessionMapper.selectOne(Wrappers.<TriageSession>query()
                .eq("consultation_id", record.getId())
                .last("limit 1"));
        if (session == null) return "当前问诊记录尚未生成导诊会话";
        if ("closed".equalsIgnoreCase(session.getStatus())) return "当前导诊会话已关闭";

        String content = trimToNull(vo.getContent());
        if (content == null) return "请输入想补充给 AI 的内容";

        List<TriageMessage> historyMessages = triageMessageMapper.selectList(Wrappers.<TriageMessage>query()
                .eq("session_id", session.getId())
                .orderByAsc("sort")
                .orderByAsc("id"));
        List<ConsultationRecordAnswer> answers = consultationRecordAnswerMapper.selectList(Wrappers.<ConsultationRecordAnswer>query()
                .eq("consultation_id", record.getId())
                .orderByAsc("sort")
                .orderByAsc("id"));
        List<ConsultationRecommendDoctorVO> doctorCandidates = parseDoctorCandidates(record.getId());

        Date now = new Date();
        int nextSort = nextSort(session.getId());
        TriageMessage userMessage = new TriageMessage(
                null,
                session.getId(),
                "user",
                "ai_user_followup",
                "患者补充说明",
                content,
                buildUserStructuredContent(record.getId(), content),
                nextSort,
                now
        );

        List<TriageMessage> contextMessages = new ArrayList<>(historyMessages);
        contextMessages.add(userMessage);

        AiTriageAdvice advice = aiTriageService.continueConversation(AiTriageContext.builder()
                .record(record)
                .answers(answers)
                .doctorCandidates(doctorCandidates)
                .triageMessages(contextMessages)
                .userMessage(content)
                .build());
        if (advice == null) return "AI 导诊暂时无法响应，请稍后重试";

        List<TriageMessage> messages = new ArrayList<>();
        messages.add(userMessage);
        messages.addAll(buildAssistantMessages(session.getId(), record.getId(), advice, nextSort + 10, now));
        if (messages.size() <= 1) return "AI 导诊暂时没有生成有效回复";

        for (TriageMessage message : messages) {
            if (triageMessageMapper.insert(message) <= 0) return "导诊会话消息保存失败，请稍后重试";
        }

        session.setMessageCount(defaultInt(session.getMessageCount()) + messages.size());
        session.setUpdateTime(now);
        if (!StringUtils.hasText(session.getStatus())) session.setStatus("completed");
        triageSessionMapper.updateById(session);
        return null;
    }

    private List<TriageMessage> buildAssistantMessages(Integer sessionId,
                                                       Integer consultationId,
                                                       AiTriageAdvice advice,
                                                       int startSort,
                                                       Date now) {
        List<TriageMessage> messages = new ArrayList<>();

        String replyContent = buildReplyContent(advice);
        if (StringUtils.hasText(replyContent)) {
            messages.add(new TriageMessage(
                    null,
                    sessionId,
                    "assistant",
                    "ai_chat_reply",
                    "AI 导诊回复",
                    replyContent,
                    buildReplyStructuredContent(consultationId, advice),
                    startSort,
                    now
            ));
            startSort += 10;
        }

        String questionContent = buildQuestionContent(advice.getNextQuestions());
        if (StringUtils.hasText(questionContent)) {
            messages.add(new TriageMessage(
                    null,
                    sessionId,
                    "assistant",
                    "ai_followup_questions",
                    "AI 建议补充信息",
                    questionContent,
                    buildQuestionStructuredContent(consultationId, advice),
                    startSort,
                    now
            ));
        }
        return messages;
    }

    private String buildReplyContent(AiTriageAdvice advice) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(advice.getReply())) segments.add(advice.getReply().trim());
        if (!StringUtils.hasText(advice.getReply()) && StringUtils.hasText(advice.getSummary())) {
            segments.add("当前补充判断：" + advice.getSummary().trim());
        }
        if (!isEmptyList(advice.getRiskFlags())) {
            segments.add("风险提示：" + String.join("、", advice.getRiskFlags()));
        }
        if (StringUtils.hasText(advice.getRecommendedVisitType())) {
            segments.add("建议方式：" + visitTypeLabel(advice.getRecommendedVisitType()));
        }
        if (StringUtils.hasText(advice.getRecommendedDepartmentName())) {
            segments.add("建议科室：" + advice.getRecommendedDepartmentName().trim());
        }
        if (StringUtils.hasText(advice.getDoctorRecommendationReason())) {
            segments.add("推荐依据：" + advice.getDoctorRecommendationReason().trim());
        }
        if (advice.getSuggestOfflineImmediately() != null && advice.getSuggestOfflineImmediately() == 1) {
            segments.add("如症状持续加重，请尽快线下就医。");
        }
        if (advice.getShouldEscalateToHuman() != null && advice.getShouldEscalateToHuman() == 1) {
            segments.add("建议尽快让医生进一步查看当前情况。");
        }
        return segments.isEmpty() ? null : String.join("\n", segments);
    }

    private String buildQuestionContent(List<String> nextQuestions) {
        if (isEmptyList(nextQuestions)) return null;
        return nextQuestions.stream()
                .filter(StringUtils::hasText)
                .limit(3)
                .map(item -> "• " + item.trim())
                .collect(Collectors.joining("\n"));
    }

    private String buildUserStructuredContent(Integer consultationId, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("consultationId", consultationId);
        payload.put("content", content);
        return JSON.toJSONString(payload);
    }

    private String buildReplyStructuredContent(Integer consultationId, AiTriageAdvice advice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("consultationId", consultationId);
        payload.put("promptVersion", properties.getPromptVersion());
        payload.put("source", "deepseek");
        payload.put("reply", advice.getReply());
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

    private String buildQuestionStructuredContent(Integer consultationId, AiTriageAdvice advice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("consultationId", consultationId);
        payload.put("promptVersion", properties.getPromptVersion());
        payload.put("source", "deepseek");
        payload.put("nextQuestions", advice.getNextQuestions());
        return JSON.toJSONString(payload);
    }

    private List<ConsultationRecommendDoctorVO> parseDoctorCandidates(int consultationId) {
        TriageResult triageResult = triageResultMapper.selectOne(Wrappers.<TriageResult>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("is_final")
                .orderByDesc("id")
                .last("limit 1"));
        if (triageResult == null || !StringUtils.hasText(triageResult.getDoctorCandidatesJson())) return List.of();
        try {
            List<ConsultationRecommendDoctorVO> list = JSON.parseArray(triageResult.getDoctorCandidatesJson(), ConsultationRecommendDoctorVO.class);
            return list == null ? List.of() : list;
        } catch (Exception ignored) {
            return List.of();
        }
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
