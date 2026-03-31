package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorReplyTemplateCreateVO {
    @NotBlank
    @Pattern(regexp = "handle_summary|medical_advice|follow_up_plan|patient_instruction|followup_summary|followup_advice|followup_next_step")
    String sceneType;

    @NotBlank
    @Length(max = 100)
    String title;

    @NotBlank
    @Length(max = 2000)
    String content;

    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;

    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
