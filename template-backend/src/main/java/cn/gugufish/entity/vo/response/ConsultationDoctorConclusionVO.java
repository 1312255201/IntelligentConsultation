package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationDoctorConclusionVO {
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String conditionLevel;
    String disposition;
    String diagnosisDirection;
    String conclusionTagsJson;
    Integer needFollowUp;
    Integer followUpWithinDays;
    Integer isConsistentWithAi;
    String patientInstruction;
    Date createTime;
    Date updateTime;
}
