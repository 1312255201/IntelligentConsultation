package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class PatientMedicalHistoryVO {
    Integer id;
    Integer patientId;
    String allergyHistory;
    String pastHistory;
    String chronicHistory;
    String surgeryHistory;
    String familyHistory;
    String medicationHistory;
    String pregnancyStatus;
    String lactationStatus;
    String infectiousHistory;
    Date createTime;
    Date updateTime;
}
