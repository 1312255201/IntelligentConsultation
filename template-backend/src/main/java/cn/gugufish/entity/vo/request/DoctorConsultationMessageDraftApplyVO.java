package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorConsultationMessageDraftApplyVO {
    @NotNull
    @Positive
    Integer recordId;

    @NotNull
    @Positive
    Integer logId;

    @NotNull
    @Pattern(regexp = "replace|append|compose", flags = Pattern.Flag.CASE_INSENSITIVE)
    String applyMode;

    @Pattern(regexp = "message_opening|message_clarify|message_check_result|message_follow_up", flags = Pattern.Flag.CASE_INSENSITIVE)
    String templateSceneType;

    @Positive
    Integer templateId;

    @Length(max = 100)
    String templateTitle;
}
