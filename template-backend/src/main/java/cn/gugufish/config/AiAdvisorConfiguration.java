package cn.gugufish.config;

import cn.gugufish.ai.AiAdvisorProperties;
import cn.gugufish.ai.advisor.MedicalLongTermMemoryAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring AI Advisor 链核心配置。
 *
 * <p>组装顺序决定了拦截器链的执行逻辑：</p>
 * <ol>
 *   <li>MedicalLongTermMemoryAdvisor (order=10)  → 最先注入长期记忆到 Prompt</li>
 *   <li>MessageChatMemoryAdvisor    (order=100) → 注入短期滑动窗口上下文</li>
 *   <li>QuestionAnswerAdvisor       (order=200) → RAG 检索医学知识库增强 Prompt</li>
 * </ol>
 *
 * <p>before 阶段按 order 从小到大执行，after 阶段按 order 从大到小执行。</p>
 */
@Slf4j
@Configuration
public class AiAdvisorConfiguration {

    /**
     * 短期会话记忆（滑动窗口），基于 InMemory 存储。
     * <p>每个 conversationId 独立维护一份消息队列，
     * 超过 maxMessages 后自动丢弃最早的消息，但 System Message 始终保留。</p>
     */
    @Bean
    public ChatMemory chatMemory(AiAdvisorProperties props) {
        return MessageWindowChatMemory.builder()
                .maxMessages(props.getMemoryWindowSize())
                .build();
    }

    /**
     * 简单向量存储 —— 基于内存的向量数据库。
     * <p>启动时由 MedicalKnowledgeLoader 预加载医学知识文档。
     * 生产环境建议替换为 PGVector / Redis Vector 等持久化方案。</p>
     *
     * <p>这里不再使用 {@code @ConditionalOnBean(EmbeddingModel.class)}：
     * 该写法放在普通业务配置类中，可能因为 Spring Boot 自动配置装配顺序而过早判定，
     * 导致 OllamaEmbeddingModel 虽然最终会被自动配置出来，但当前 Bean 定义已经被跳过，
     * 最终表现为“本地 Ollama 正常、EmbeddingModel 实际存在、VectorStore 却没有创建”。</p>
     *
     * <p>改为直接通过方法参数依赖 EmbeddingModel，让容器在真正实例化本 Bean 时再解析依赖，
     * 从而避免用户配置类与自动配置类之间的条件判定时序问题。</p>
     */
    @Bean
    public VectorStore medicalVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 核心：组装 ChatClient（带完整 Advisor 拦截器链）。
     *
     * <p>摒弃旧代码中 ChatClient.create(chatModel).prompt()... 的即时创建模式，
     * 改用 ChatClient.builder() 全局配置模式，在构造阶段就织入所有 Advisor。</p>
     */
    @Bean
    public ChatClient advisorChatClient(
            DeepSeekChatModel chatModel,
            ChatMemory chatMemory,
            ObjectProvider<VectorStore> vectorStoreProvider,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            AiAdvisorProperties props) {

        // ========== 构建 Advisor 链 ==========
        var builder = ChatClient.builder(chatModel);

        // ① 长期记忆 Advisor（自定义，order=10）
        // 从 Redis 读取该患者的过敏史、红旗症状等关键事实，
        // 注入 System Prompt；对话后提取新发现的关键事实写回 Redis
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            builder.defaultAdvisors(
                    new MedicalLongTermMemoryAdvisor(redisTemplate, props)
            );
            log.info("✅ MedicalLongTermMemoryAdvisor 已激活（Redis 长期记忆）");
        }

        // ② 短期记忆 Advisor（order=100）
        // 自动维护每个 conversationId 的多轮对话滑动窗口
        builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        );
        log.info("✅ MessageChatMemoryAdvisor 已激活（窗口大小={}）", props.getMemoryWindowSize());

        // ③ RAG 防幻觉 Advisor（order=200）
        // 从向量数据库检索最相关的医学知识文档，注入 Prompt 约束 LLM 输出
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            builder.defaultAdvisors(
                    QuestionAnswerAdvisor.builder(vectorStore)
                            .searchRequest(SearchRequest.builder()
                                    .topK(props.getRagTopK())
                                    .similarityThreshold(props.getRagSimilarityThreshold())
                                    .build())
                            .build()
            );
            log.info("✅ QuestionAnswerAdvisor 已激活（RAG top-k={}, 阈值={}）",
                    props.getRagTopK(), props.getRagSimilarityThreshold());
        }

        return builder.build();
    }
}
