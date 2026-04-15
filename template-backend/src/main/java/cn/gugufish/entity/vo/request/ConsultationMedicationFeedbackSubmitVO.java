package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class ConsultationMedicationFeedbackSubmitVO {
    @NotNull
    @Positive
    Integer recordId;

    @Positive
    Integer prescriptionId;

    @Positive
    Integer medicineId;

    @Length(max = 100)
    String medicineName;

    @Pattern(regexp = "improved|limited|adverse_reaction|other")
    String feedbackType;

    @Pattern(regexp = "mild|medium|high")
    String severityLevel;

    @Pattern(regexp = "continued|paused|stopped|consulting|other")
    String actionTaken;

    @Length(max = 1000)
    String feedbackSummary;

    @Length(max = 300)
    String doctorQuestion;

    @Size(max = 6)
    List<String> attachments;
}
