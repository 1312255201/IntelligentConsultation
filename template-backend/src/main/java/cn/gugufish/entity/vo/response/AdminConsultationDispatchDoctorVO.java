package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminConsultationDispatchDoctorVO {
    Integer doctorId;
    String doctorName;
    String departmentName;
    Integer totalSuggestedCount;
    Integer waitingAcceptCount;
    Integer acceptedCount;
    Integer lostCount;
    String hitRateText;
    String averageClaimDurationText;
}
