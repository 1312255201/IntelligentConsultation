package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorConsultationServiceFeedbackHandleSubmitVO {
    @NotNull
    @Positive
    Integer consultationId;

    @NotNull
    @Min(0)
    @Max(1)
    Integer doctorHandleStatus;

    @Length(max = 1000)
    String handleRemark;
}
