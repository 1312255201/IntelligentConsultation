package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

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

    @Pattern(regexp = "low|medium|high|critical")
    String conditionLevel;

    @Pattern(regexp = "observe|online_followup|offline_visit|emergency")
    String disposition;

    @Length(max = 100)
    String diagnosisDirection;

    List<String> conclusionTags;

    @Min(0)
    @Max(1)
    Integer needFollowUp;

    @Positive
    Integer followUpWithinDays;

    @Min(0)
    @Max(1)
    Integer isConsistentWithAi;

    List<String> aiMismatchReasons;

    @Length(max = 500)
    String aiMismatchRemark;

    @Length(max = 500)
    String patientInstruction;

    List<DoctorPrescriptionItemSubmitVO> prescriptions;

    @Positive
    Integer aiLogId;
}
