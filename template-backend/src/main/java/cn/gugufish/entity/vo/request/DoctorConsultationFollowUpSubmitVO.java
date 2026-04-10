package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorConsultationFollowUpSubmitVO {
    @NotNull
    @Positive
    Integer consultationId;

    @NotBlank
    @Pattern(regexp = "platform|phone|offline|other")
    String followUpType;

    @NotBlank
    @Pattern(regexp = "improved|stable|worsened|other")
    String patientStatus;

    @NotBlank
    @Length(max = 500)
    String summary;

    @Length(max = 1000)
    String advice;

    @Length(max = 500)
    String nextStep;

    @Min(0)
    @Max(1)
    Integer needRevisit;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    String nextFollowUpDate;

    @Positive
    Integer aiLogId;
}
