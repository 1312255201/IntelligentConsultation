package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsultationDispatchConfigSaveVO {
    @NotNull
    @Min(0)
    @Max(300)
    Integer visitTypeWeight;
    @NotNull
    @Min(0)
    @Max(300)
    Integer scheduleWeight;
    @NotNull
    @Min(0)
    @Max(300)
    Integer capacityWeight;
    @NotNull
    @Min(0)
    @Max(300)
    Integer workloadWeight;
    @NotNull
    @Min(0)
    @Max(300)
    Integer tagMatchWeight;
    @NotNull
    @Min(0)
    @Max(30)
    Integer tagMatchScorePerHit;
    @NotNull
    @Min(1)
    @Max(10)
    Integer maxMatchedTags;
    @NotNull
    @Min(1)
    @Max(10)
    Integer recommendDoctorLimit;
    @NotNull
    @Min(0)
    @Max(100)
    Integer minRecommendationScore;
    @NotNull
    @Min(1)
    @Max(100)
    Integer maxRecommendationScore;
    @NotNull
    @Min(1)
    @Max(168)
    Integer waitingOverdueHours;
}
