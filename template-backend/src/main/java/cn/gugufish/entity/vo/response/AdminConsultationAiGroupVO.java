package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminConsultationAiGroupVO {
    String groupName;
    Integer totalCount;
    Integer comparedCount;
    Integer consistentCount;
    Integer mismatchCount;
    Integer pendingCount;
    String coverageText;
    String consistentRateText;
}
