package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationServiceFeedbackVO {
    Integer id;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    String patientName;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    Integer serviceScore;
    Integer isResolved;
    String feedbackText;
    Integer doctorHandleStatus;
    String doctorHandleRemark;
    Integer doctorHandleAccountId;
    Integer doctorHandleDoctorId;
    String doctorHandleDoctorName;
    Date doctorHandleTime;
    Integer status;
    Date createTime;
    Date updateTime;
}
