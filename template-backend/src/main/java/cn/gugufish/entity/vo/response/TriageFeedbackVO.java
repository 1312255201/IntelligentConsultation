package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TriageFeedbackVO {
    Integer id;
    Integer sessionId;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    Integer userScore;
    Integer isAdopted;
    String feedbackText;
    Integer manualCorrectDepartmentId;
    String manualCorrectDepartmentName;
    Integer manualCorrectDoctorId;
    String manualCorrectDoctorName;
    Integer status;
    Date createTime;
    Date updateTime;
}
