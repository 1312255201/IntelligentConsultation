package cn.gugufish.entity.vo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorConsultationFollowUpDraftVO {
    Integer logId;
    String followUpType;
    String patientStatus;
    String summary;
    String advice;
    String nextStep;
    Integer needRevisit;
    String nextFollowUpDate;
    String generationSummary;
    List<String> riskFlags;
    String promptVersion;
    String source;
    Integer fallback;
}
