package cn.gugufish.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Advisor 链核心配置属性。
 * 控制短期记忆窗口、长期记忆持久化与 RAG 检索行为。
 */
@Data
@Component
@ConfigurationProperties(prefix = "consultation.ai.advisor")
public class AiAdvisorProperties {

    /** 短期对话记忆滑动窗口大小（消息条数），超过后最早的消息被丢弃 */
    private int memoryWindowSize = 20;

    /** 长期记忆 Redis key 前缀 */
    private String longTermMemoryPrefix = "ai:ltm:patient:";

    /** 长期记忆过期天数 */
    private int longTermMemoryTtlDays = 180;

    /** RAG 检索返回的 Top-K 文档数 */
    private int ragTopK = 4;

    /** RAG 检索的最低相似度阈值 */
    private double ragSimilarityThreshold = 0.72;

    /** 医学知识库资源路径 */
    private String knowledgeBasePath = "classpath:medical-knowledge/";
}
