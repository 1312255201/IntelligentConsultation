package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorConsultationFormDraftApplyVO {
    @NotNull
    @Positive
    Integer recordId;

    @NotNull
    @Positive
    Integer logId;

    @Pattern(regexp = "replace|append|compose", flags = Pattern.Flag.CASE_INSENSITIVE)
    String applyMode;

    @Pattern(regexp = "handle_form|conclusion_form|follow_up_form", flags = Pattern.Flag.CASE_INSENSITIVE)
    String applyTarget;

    @Pattern(regexp = "handle_summary|medical_advice|follow_up_plan|patient_instruction|followup_summary|followup_advice|followup_next_step", flags = Pattern.Flag.CASE_INSENSITIVE)
    String templateSceneType;

    @Positive
    Integer templateId;

    @Length(max = 100)
    String templateTitle;
}
