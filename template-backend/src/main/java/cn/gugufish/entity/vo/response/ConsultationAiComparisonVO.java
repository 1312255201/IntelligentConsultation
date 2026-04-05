package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationAiComparisonVO {
    String overallStatus;
    String summary;
    String aiConditionLevel;
    String aiDisposition;
    String aiDepartmentName;
    String aiReasonText;
    String aiConfidenceText;
    String aiFollowUpText;
    List<String> aiRiskFlags;
    List<String> aiRecommendedDoctors;
    String conditionLevelStatus;
    String dispositionStatus;
    String followUpStatus;
}
