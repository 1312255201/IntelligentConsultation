package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class ConsultationSmartDispatchVO {
    String status;
    String statusText;
    String hint;
    String recommendationSource;
    Integer candidateCount;
    Integer suggestedDoctorId;
    String suggestedDoctorName;
    String suggestedDoctorTitle;
    String suggestedDoctorPhoto;
    String suggestedDoctorExpertise;
    String suggestedDoctorNextScheduleText;
    Integer claimedDoctorId;
    String claimedDoctorName;
    String recommendationReason;
}
