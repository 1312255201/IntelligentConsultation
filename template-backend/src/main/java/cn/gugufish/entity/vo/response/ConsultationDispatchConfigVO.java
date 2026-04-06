package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationDispatchConfigVO {
    Integer id;
    Integer visitTypeWeight;
    Integer scheduleWeight;
    Integer capacityWeight;
    Integer workloadWeight;
    Integer tagMatchWeight;
    Integer tagMatchScorePerHit;
    Integer maxMatchedTags;
    Integer recommendDoctorLimit;
    Integer minRecommendationScore;
    Integer maxRecommendationScore;
    Integer waitingOverdueHours;
    Date updateTime;
}
