package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationAiAuditItemVO {
    Integer messageId;
    Integer sessionId;
    String sessionNo;
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String consultationStatus;
    String triageLevelName;
    String triageActionType;
    String messageType;
    String title;
    String content;
    String structuredContent;
    ConsultationDoctorHandleVO doctorHandle;
    ConsultationDoctorConclusionVO doctorConclusion;
    Date createTime;
}
