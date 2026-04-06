package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminConsultationDispatchWaitVO {
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String suggestedDoctorName;
    String recommendationReason;
    Date createTime;
    String waitingDurationText;
}
