package cn.gugufish.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "consultation.ai.triage")
public class AiTriageProperties {
    private boolean enabled = true;
    private String promptVersion = "deepseek-triage-v1";
    private int doctorCandidateLimit = 3;
}
