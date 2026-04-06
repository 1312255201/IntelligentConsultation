package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class ConsultationDispatchBatchCompareItemVO {
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String title;
    String triageLevelName;
    String triageActionType;
    Integer savedTopDoctorId;
    String savedTopDoctorName;
    String savedTopDoctorTitle;
    Integer savedTopDoctorScore;
    Integer currentTopDoctorId;
    String currentTopDoctorName;
    String currentTopDoctorTitle;
    Integer currentTopDoctorScore;
    Integer sharedDoctorCount;
    Integer savedDoctorCount;
    Integer currentDoctorCount;
    Boolean topDoctorChanged;
    Boolean bothNoRecommendation;
}
