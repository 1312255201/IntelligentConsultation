package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationMedicationFeedbackVO {
    Integer id;
    Integer consultationId;
    Integer prescriptionId;
    Integer medicineId;
    String medicineName;
    Integer accountId;
    Integer patientId;
    String patientName;
    String feedbackType;
    String severityLevel;
    String actionTaken;
    String feedbackSummary;
    String doctorQuestion;
    String attachmentsJson;
    Integer status;
    Date createTime;
    Date updateTime;
    List<String> attachments;
}
