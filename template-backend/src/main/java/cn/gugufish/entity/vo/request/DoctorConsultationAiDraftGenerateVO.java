package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorConsultationAiDraftGenerateVO {
    @NotNull
    @Positive
    Integer recordId;

    @Pattern(regexp = "doctor_summary|medical_advice|follow_up_plan|patient_instruction|followup_summary|followup_advice|followup_next_step",
            flags = Pattern.Flag.CASE_INSENSITIVE)
    String regenerateField;

    @Length(max = 500)
    String rewriteRequirement;

    @Length(max = 12000)
    String currentDraftJson;
}
