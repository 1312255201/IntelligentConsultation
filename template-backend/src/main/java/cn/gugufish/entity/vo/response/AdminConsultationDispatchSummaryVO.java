package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminConsultationDispatchSummaryVO {
    Integer totalRecordCount;
    Integer suggestedCount;
    Integer waitingAcceptCount;
    Integer claimedBySuggestedCount;
    Integer claimedByOtherCount;
    Integer departmentQueueCount;
    Integer overdueWaitingCount;
    Integer overdueThresholdHours;
    String suggestionCoverageText;
    String suggestedHitRateText;
    String claimedByOtherRateText;
    String averageClaimDurationText;
    List<AdminConsultationDispatchDoctorVO> doctorBreakdown;
    List<AdminConsultationDispatchWaitVO> recentWaitingRecords;
}
