package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminConsultationAiFieldVO {
    String fieldKey;
    String fieldLabel;
    Integer totalCount;
    Integer comparedCount;
    Integer matchCount;
    Integer mismatchCount;
    Integer pendingCount;
    String mismatchRateText;
    List<AdminConsultationAiReasonVO> mismatchReasonBreakdown;
}
