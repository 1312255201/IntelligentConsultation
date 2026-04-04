package cn.gugufish.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "summary",
        "riskFlags",
        "recommendedDepartmentName",
        "recommendedVisitType",
        "recommendedDoctorIds",
        "recommendedDoctorNames",
        "doctorRecommendationReason",
        "nextQuestions",
        "shouldEscalateToHuman",
        "suggestOfflineImmediately",
        "confidenceScore"
})
public class AiTriageAdvice {
    String reply;
    String summary;
    List<String> riskFlags;
    String recommendedDepartmentName;
    String recommendedVisitType;
    List<Integer> recommendedDoctorIds;
    List<String> recommendedDoctorNames;
    String doctorRecommendationReason;
    List<String> nextQuestions;
    Integer shouldEscalateToHuman;
    Integer suggestOfflineImmediately;
    BigDecimal confidenceScore;
}
