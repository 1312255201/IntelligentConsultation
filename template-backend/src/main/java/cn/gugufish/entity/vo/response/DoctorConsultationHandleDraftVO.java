package cn.gugufish.entity.vo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorConsultationHandleDraftVO {
    Integer logId;
    String doctorSummary;
    String medicalAdvice;
    String followUpPlan;
    String conditionLevel;
    String disposition;
    String diagnosisDirection;
    List<String> conclusionTags;
    Integer needFollowUp;
    Integer followUpWithinDays;
    String patientInstruction;
    String generationSummary;
    List<String> riskFlags;
    String promptVersion;
    String source;
    Integer fallback;
}
