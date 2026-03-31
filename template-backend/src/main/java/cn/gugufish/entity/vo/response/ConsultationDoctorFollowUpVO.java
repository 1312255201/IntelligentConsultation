package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationDoctorFollowUpVO {
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String followUpType;
    String patientStatus;
    String summary;
    String advice;
    String nextStep;
    Integer needRevisit;
    Date nextFollowUpDate;
    Date createTime;
    Date updateTime;
}
