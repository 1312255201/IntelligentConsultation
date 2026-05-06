package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsultationIntakeRouteRequestVO {
    @Positive
    Integer patientId;

    @NotBlank
    @Size(max = 200)
    String chiefComplaint;

    @Size(max = 32)
    String matchMode;
}
