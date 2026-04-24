package cn.gugufish.ai.advisor;

import cn.gugufish.ai.AiAdvisorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 医疗长期记忆 Advisor —— 解决跨会话关键医学事实遗忘问题。
 *
 * <h3>设计原理</h3>
 * <p>
 * 模拟 Spring AI 2.0 的 AutoMemoryToolsAdvisor 理念，
 * 但基于 Redis（项目已有）实现，无需升级到 Spring AI 2.0-M4。
 * </p>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li><b>Before 阶段：</b>根据 patientId 从 Redis 读取该患者的长期记忆
 *       （过敏史、红旗症状、慢性病等），作为额外 System Message 注入 Prompt</li>
 *   <li><b>After 阶段：</b>分析 LLM 响应，提取新发现的关键医学事实
 *       （如 "患者对青霉素严重过敏"），写入 Redis 长期记忆</li>
 * </ol>
 *
 * <h3>Redis 数据结构</h3>
 * <pre>Key:   ai:ltm:patient:{patientId}
 * Type:  Hash
 * Fields:  allergy_flags, risk_flags, chronic_conditions, critical_notes
 * TTL:   180 天（可配置）</pre>
 */
@Slf4j
public class MedicalLongTermMemoryAdvisor implements CallAdvisor, StreamAdvisor {

    /** Advisor 运行时参数 key —— 调用方通过此 key 传入 patientId */
    public static final String PATIENT_ID_KEY = "medical_patient_id";

    private static final int ORDER = 10; // 最先执行，在 MessageChatMemoryAdvisor 之前

    private final StringRedisTemplate redisTemplate;
    private final AiAdvisorProperties props;

    // 用于从 LLM 响应中提取关键医学事实的模式
    private static final Pattern ALLERGY_PATTERN = Pattern.compile(
            "(过敏|allergic|allergy)[^。，,;；]*[。，,;；]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern RISK_PATTERN = Pattern.compile(
            "(红旗|高风险|high.?risk|危重|急诊|胸痛伴呼吸困难|大出血|意识障碍|高热抽搐)[^。，,;；]*[。，,;；]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public MedicalLongTermMemoryAdvisor(StringRedisTemplate redisTemplate,
                                         AiAdvisorProperties props) {
        this.redisTemplate = redisTemplate;
        this.props = props;
    }

    @Override
    public String getName() {
        return "MedicalLongTermMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    // ==================== 同步调用拦截 ====================

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // ---------- BEFORE：注入长期记忆 ----------
        ChatClientRequest enrichedRequest = injectLongTermMemory(request);

        // 继续执行下一个 Advisor（MessageChatMemoryAdvisor → QuestionAnswerAdvisor → LLM）
        ChatClientResponse response = chain.nextCall(enrichedRequest);

        // ---------- AFTER：提取并持久化新关键事实 ----------
        extractAndPersistFacts(request, response);

        return response;
    }

    // ==================== 流式调用拦截 ====================

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest enrichedRequest = injectLongTermMemory(request);
        return chain.nextStream(enrichedRequest);
        // 流式模式下 after 阶段的事实提取可通过 doOnComplete 实现，此处简化省略
    }

    // ==================== 核心方法：注入长期记忆 ====================

    private ChatClientRequest injectLongTermMemory(ChatClientRequest request) {
        String patientId = extractPatientId(request);
        if (patientId == null) return request;

        String redisKey = props.getLongTermMemoryPrefix() + patientId;
        Map<Object, Object> memory = redisTemplate.opsForHash().entries(redisKey);
        if (memory.isEmpty()) return request;

        // 将长期记忆格式化为 System Message 注入
        StringBuilder memoryPrompt = new StringBuilder();
        memoryPrompt.append("\n\n【患者长期医学记忆 — 以下信息来自历史会话，请务必纳入考量】\n");

        appendIfPresent(memoryPrompt, memory, "allergy_flags", "⚠️ 过敏史");
        appendIfPresent(memoryPrompt, memory, "risk_flags", "🚩 历史红旗症状");
        appendIfPresent(memoryPrompt, memory, "chronic_conditions", "📋 慢性病史");
        appendIfPresent(memoryPrompt, memory, "critical_notes", "📌 关键备注");

        memoryPrompt.append("【长期记忆结束】\n");

        // 在现有消息列表头部追加一条 System Message，构建新 Prompt
        Prompt originalPrompt = request.prompt();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(memoryPrompt.toString()));
        messages.addAll(originalPrompt.getInstructions());

        Prompt newPrompt = originalPrompt.mutate().messages(messages).build();

        log.debug("长期记忆已注入（患者={}，字段数={}）", patientId, memory.size());

        return request.mutate()
                .prompt(newPrompt)
                .build();
    }

    // ==================== 核心方法：提取关键事实写入长期记忆 ====================

    private void extractAndPersistFacts(ChatClientRequest request, ChatClientResponse response) {
        String patientId = extractPatientId(request);
        if (patientId == null) return;

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResults().isEmpty()) return;

        String content = chatResponse.getResults().get(0).getOutput().getText();
        if (content == null || content.isBlank()) return;

        String redisKey = props.getLongTermMemoryPrefix() + patientId;
        Map<String, String> newFacts = new HashMap<>();

        // 提取过敏信息
        extractAndMerge(content, ALLERGY_PATTERN, redisKey, "allergy_flags", newFacts);
        // 提取高风险信号
        extractAndMerge(content, RISK_PATTERN, redisKey, "risk_flags", newFacts);

        if (!newFacts.isEmpty()) {
            redisTemplate.opsForHash().putAll(redisKey, newFacts);
            redisTemplate.expire(redisKey,
                    Duration.ofDays(props.getLongTermMemoryTtlDays()));
            log.info("长期记忆已更新（患者={}，新增字段={}）", patientId, newFacts.keySet());
        }
    }

    private void extractAndMerge(String content, Pattern pattern,
                                  String redisKey, String field,
                                  Map<String, String> newFacts) {
        Matcher matcher = pattern.matcher(content);
        Set<String> facts = new LinkedHashSet<>();

        // 先加载已有事实
        Object existing = redisTemplate.opsForHash().get(redisKey, field);
        if (existing != null) {
            facts.addAll(List.of(existing.toString().split("\\|")));
        }

        // 新增匹配到的事实
        int added = 0;
        while (matcher.find() && facts.size() < 10) {
            String fact = matcher.group().trim();
            if (fact.length() > 5 && facts.add(fact)) {
                added++;
            }
        }

        if (added > 0) {
            newFacts.put(field, String.join("|", facts));
        }
    }

    // ==================== 辅助方法 ====================

    private String extractPatientId(ChatClientRequest request) {
        Object id = request.context().get(PATIENT_ID_KEY);
        return id != null ? id.toString() : null;
    }

    private void appendIfPresent(StringBuilder sb, Map<Object, Object> memory,
                                  String key, String label) {
        Object value = memory.get(key);
        if (value != null && !value.toString().isBlank()) {
            sb.append("- ").append(label).append("：")
              .append(value.toString().replace("|", "；"))
              .append("\n");
        }
    }
}
