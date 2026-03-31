package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AdminConsultationRecordVO {
    Integer id;
    String consultationNo;
    Integer accountId;
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
    List<TriageRuleHitLogVO> ruleHits;
    ConsultationDoctorAssignmentVO doctorAssignment;
    ConsultationDoctorHandleVO doctorHandle;
    ConsultationDoctorConclusionVO doctorConclusion;
    List<ConsultationDoctorFollowUpVO> doctorFollowUps;
    TriageSessionVO triageSession;
    TriageResultVO triageResult;
    TriageFeedbackVO triageFeedback;
}
