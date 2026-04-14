package cn.gugufish.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Data
@Component
@ConfigurationProperties(prefix = "consultation.ai.triage")
public class AiTriageProperties {
    private boolean enabled = true;
    private String promptVersion = "deepseek-triage-v1";
    private int doctorCandidateLimit = 3;
    private boolean defaultEnabled = true;
    private String defaultPromptVersion = "deepseek-triage-v1";
    private int defaultDoctorCandidateLimit = 3;

    @PostConstruct
    public void captureDefaults() {
        this.defaultEnabled = this.enabled;
        this.defaultPromptVersion = this.promptVersion;
        this.defaultDoctorCandidateLimit = this.doctorCandidateLimit;
    }
}
