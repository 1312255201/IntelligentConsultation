package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorConsultationHandleSubmitVO {
    @NotNull
    @Positive
    Integer consultationId;

    @NotBlank
    @Pattern(regexp = "processing|completed")
    String status;

    @Length(max = 500)
    String summary;

    @Length(max = 4000)
    String medicalAdvice;

    @Length(max = 500)
    String followUpPlan;

    @Length(max = 500)
    String internalRemark;
}
