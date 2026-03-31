package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationDoctorHandleVO {
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String status;
    String summary;
    String medicalAdvice;
    String followUpPlan;
    String internalRemark;
    Date receiveTime;
    Date completeTime;
    Date createTime;
    Date updateTime;
}
