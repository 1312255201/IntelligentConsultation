package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminConsultationAiSummaryVO {
    Integer totalRecordCount;
    Integer comparedCount;
    Integer consistentCount;
    Integer mismatchCount;
    Integer pendingCount;
    String coverageText;
    String consistentRateText;
    List<AdminConsultationAiGroupVO> departmentBreakdown;
    List<AdminConsultationAiGroupVO> categoryBreakdown;
    List<AdminConsultationAiDoctorVO> doctorBreakdown;
    List<AdminConsultationAiReasonVO> mismatchReasonBreakdown;
    List<AdminConsultationAiMismatchVO> recentMismatchRecords;
}
