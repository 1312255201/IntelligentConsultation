package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class UserPrescriptionVO {
    Integer id;
    Integer consultationId;
    String consultationNo;
    String consultationStatus;
    Integer patientId;
    String patientName;
    String consultationCategoryName;
    String doctorName;
    Integer medicineId;
    String medicineName;
    String genericName;
    String medicineCategoryName;
    String specification;
    String dosage;
    String frequency;
    Integer durationDays;
    String medicationInstruction;
    String warningSummary;
    Date createTime;
    Date updateTime;
}
