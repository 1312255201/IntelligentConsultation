package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorPrescriptionItemSubmitVO {
    @Positive
    Integer medicineId;

    @Length(max = 100)
    String dosage;

    @Length(max = 100)
    String frequency;

    @Positive
    Integer durationDays;

    @Length(max = 255)
    String medicationInstruction;
}
