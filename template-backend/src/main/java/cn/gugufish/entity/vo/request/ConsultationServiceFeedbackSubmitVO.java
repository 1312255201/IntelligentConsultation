package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ConsultationServiceFeedbackSubmitVO {
    @NotNull
    @Positive
    Integer recordId;
    @NotNull
    @Min(1)
    @Max(5)
    Integer serviceScore;
    @NotNull
    @Min(0)
    @Max(1)
    Integer isResolved;
    @Length(max = 1000)
    String feedbackText;
}
