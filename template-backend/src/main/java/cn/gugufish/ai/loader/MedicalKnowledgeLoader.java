package cn.gugufish.ai.loader;

import cn.gugufish.ai.AiAdvisorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 启动时加载医学知识库到向量数据库。
 *
 * <p>从 classpath:medical-knowledge/ 目录读取所有 .txt 文件，
 * 按段落切分后写入 VectorStore，供 QuestionAnswerAdvisor 检索使用。</p>
 *
 * <h3>知识库文件格式示例（.txt）</h3>
 * <pre>
 * # 高血压临床指南摘要
 *
 * 高血压的诊断标准为收缩压 &ge; 140 mmHg 和/或舒张压 &ge; 90 mmHg。
 * 对于初诊患者，建议至少在三次不同日期测量血压以确认诊断。
 *
 * # 药物治疗原则
 *
 * 一线降压药物包括 ACEI/ARB、CCB、利尿剂。
 * 对于合并糖尿病的高血压患者，优先选用 ACEI/ARB 类药物。
 * </pre>
 */
@Slf4j
@Component
public class MedicalKnowledgeLoader implements CommandLineRunner {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final AiAdvisorProperties props;

    public MedicalKnowledgeLoader(ObjectProvider<VectorStore> vectorStoreProvider,
                                   AiAdvisorProperties props) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("⚠️ VectorStore 不可用（缺少 Embedding Model？），医学知识库未加载");
            return;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = props.getKnowledgeBasePath().replace("classpath:", "classpath:") + "*.txt";
            Resource[] resources = resolver.getResources(pattern);

            if (resources.length == 0) {
                log.info("ℹ️ 未找到医学知识库文件（路径={}），跳过加载", pattern);
                return;
            }

            List<Document> allDocuments = new ArrayList<>();
            for (Resource resource : resources) {
                List<Document> docs = parseResource(resource);
                allDocuments.addAll(docs);
            }

            if (!allDocuments.isEmpty()) {
                vectorStore.add(allDocuments);
                log.info("✅ 医学知识库加载完成：{} 个文件，{} 个文档片段",
                        resources.length, allDocuments.size());
            }
        } catch (Exception e) {
            log.warn("⚠️ 医学知识库加载失败：{}", e.getMessage());
        }
    }

    /**
     * 按空行分段，每个段落生成一个 Document。
     */
    private List<Document> parseResource(Resource resource) throws Exception {
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }

        String fileName = resource.getFilename();
        String[] paragraphs = content.split("\\n\\s*\\n");
        List<Document> documents = new ArrayList<>();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            // 跳过标题行和过短段落
            if (trimmed.startsWith("#") || trimmed.length() < 20) continue;

            documents.add(Document.builder()
                    .text(trimmed)
                    .metadata(Map.of(
                            "source", fileName != null ? fileName : "unknown",
                            "type", "medical_knowledge"
                    ))
                    .build());
        }

        return documents;
    }
}
