package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ConsultationTriageFeedbackSubmitVO {
    @NotNull
    @Positive
    Integer recordId;
    @NotNull
    @Min(1)
    @Max(5)
    Integer userScore;
    @NotNull
    @Min(0)
    @Max(1)
    Integer isAdopted;
    @Length(max = 500)
    String feedbackText;
    @Positive
    Integer manualCorrectDepartmentId;
    @Positive
    Integer manualCorrectDoctorId;
}
