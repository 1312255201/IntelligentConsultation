package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class DoctorWorkbenchVO {
    Integer bound;
    String bindingMessage;
    Integer doctorId;
    Integer doctorStatus;
    Integer accountId;
    Integer departmentId;
    String departmentName;
    String doctorName;
    String doctorTitle;
    String photo;
    String introduction;
    String expertise;
    Integer consultationCount;
    Integer todayConsultationCount;
    Integer riskConsultationCount;
    Integer unclaimedConsultationCount;
    Integer myClaimedConsultationCount;
    Integer highPriorityUnclaimedCount;
    Integer unreadConsultationCount;
    Integer waitingReplyConsultationCount;
    Integer pendingFollowUpCount;
    Integer dueTodayFollowUpCount;
    Integer overdueFollowUpCount;
    Integer recommendedConsultationCount;
    Integer upcomingScheduleCount;
    Integer serviceTagCount;
    Integer serviceFeedbackCount;
    Integer resolvedServiceFeedbackCount;
    Integer unresolvedServiceFeedbackCount;
    Integer lowScoreServiceFeedbackCount;
    Integer attentionServiceFeedbackCount;
    Double averageServiceScore;
    Double resolvedServiceFeedbackRate;
    List<String> serviceTags;
    List<ConsultationServiceFeedbackVO> recentServiceFeedbacks;
    List<AdminConsultationRecordVO> recentConsultations;
    List<AdminConsultationRecordVO> unclaimedConsultations;
    List<AdminConsultationRecordVO> recommendedConsultations;
    List<AdminConsultationRecordVO> unreadConsultations;
    List<AdminConsultationRecordVO> waitingReplyConsultations;
    List<AdminConsultationRecordVO> pendingFollowUpConsultations;
    List<DoctorScheduleVO> upcomingSchedules;
}
