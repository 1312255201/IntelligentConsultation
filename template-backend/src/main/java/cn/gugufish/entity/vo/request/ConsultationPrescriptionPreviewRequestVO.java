package cn.gugufish.entity.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class ConsultationPrescriptionPreviewRequestVO {
    @Positive
    Integer consultationId;

    @Valid
    List<DoctorPrescriptionItemSubmitVO> prescriptions;
}
