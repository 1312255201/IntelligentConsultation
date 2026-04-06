package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationDispatchBatchCompareVO {
    Integer totalCount;
    Integer changedCount;
    Integer unchangedCount;
    Integer bothNoRecommendationCount;
    List<ConsultationDispatchBatchCompareItemVO> items;
}
