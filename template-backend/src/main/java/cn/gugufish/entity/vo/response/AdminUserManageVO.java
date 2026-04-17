package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminUserManageVO {
    Integer accountId;
    String username;
    String email;
    Date registerTime;
    Integer patientCount;
    Integer medicalHistoryCount;
    Integer consultationCount;
    Integer completedConsultationCount;
    Integer paidOrderCount;
    Integer prescriptionCount;
    Date latestConsultationTime;
}
