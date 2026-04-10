package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DoctorConsultationMessageDraftGenerateVO {
    @NotNull
    @Positive
    Integer recordId;

    @Pattern(regexp = "opening|clarify|check_result|follow_up", flags = Pattern.Flag.CASE_INSENSITIVE)
    String sceneType;
}
