package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationRecordVO {
    Integer id;
    String consultationNo;
    Integer patientId;
    String patientName;
    Integer categoryId;
    String categoryName;
    Integer departmentId;
    String departmentName;
    Integer templateId;
    String templateName;
    String title;
    String chiefComplaint;
    String healthSummary;
    String status;
    Integer answerCount;
    Integer triageLevelId;
    String triageLevelCode;
    String triageLevelName;
    String triageLevelColor;
    String triageActionType;
    String triageSuggestion;
    String triageRuleSummary;
    Date createTime;
    Date updateTime;
    List<ConsultationRecordAnswerVO> answers;
    List<ConsultationRecommendDoctorVO> recommendedDoctors;
    ConsultationDoctorAssignmentVO doctorAssignment;
    ConsultationDoctorHandleVO doctorHandle;
    ConsultationDoctorConclusionVO doctorConclusion;
    ConsultationSmartDispatchVO smartDispatch;
    ConsultationMessageSummaryVO messageSummary;
    ConsultationArchiveSummaryVO archiveSummary;
    ConsultationAiComparisonVO aiComparison;
    ConsultationPaymentVO payment;
    List<ConsultationPrescriptionVO> prescriptions;
    List<ConsultationDoctorFollowUpVO> doctorFollowUps;
    TriageSessionVO triageSession;
    TriageResultVO triageResult;
    TriageFeedbackVO triageFeedback;
    ConsultationServiceFeedbackVO serviceFeedback;
}
