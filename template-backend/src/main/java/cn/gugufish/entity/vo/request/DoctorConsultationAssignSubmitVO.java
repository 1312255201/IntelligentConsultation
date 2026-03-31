package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DoctorConsultationAssignSubmitVO {
    @NotNull
    @Positive
    Integer consultationId;
}
