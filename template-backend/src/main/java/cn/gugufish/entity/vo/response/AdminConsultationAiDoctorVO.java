package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminConsultationAiDoctorVO {
    Integer doctorId;
    String doctorName;
    String departmentName;
    Integer totalCount;
    Integer comparedCount;
    Integer consistentCount;
    Integer mismatchCount;
    Integer pendingCount;
    String coverageText;
    String consistentRateText;
    List<AdminConsultationAiReasonVO> mismatchReasonBreakdown;
}
