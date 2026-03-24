package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class PatientMedicalHistorySaveVO {
    @NotNull
    @Positive
    Integer patientId;
    @Length(max = 3000)
    String allergyHistory;
    @Length(max = 3000)
    String pastHistory;
    @Length(max = 3000)
    String chronicHistory;
    @Length(max = 3000)
    String surgeryHistory;
    @Length(max = 3000)
    String familyHistory;
    @Length(max = 3000)
    String medicationHistory;
    @Pattern(regexp = "(none|pregnant|possible|unknown)")
    String pregnancyStatus;
    @Pattern(regexp = "(none|lactating|unknown)")
    String lactationStatus;
    @Length(max = 3000)
    String infectiousHistory;
}
