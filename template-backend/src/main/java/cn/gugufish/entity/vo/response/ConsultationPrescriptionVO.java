package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationPrescriptionVO {
    Integer id;
    Integer consultationId;
    Integer doctorId;
    Integer medicineId;
    String medicineName;
    String genericName;
    String categoryName;
    String specification;
    String dosage;
    String frequency;
    Integer durationDays;
    String medicationInstruction;
    String warningSummary;
    Integer sort;
    Date createTime;
    Date updateTime;
}
