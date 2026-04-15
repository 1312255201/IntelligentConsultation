package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class ConsultationReportFeedbackSubmitVO {
    @NotNull
    @Positive
    Integer recordId;

    @Positive
    Integer suggestionId;

    @Pattern(regexp = "lab|imaging|pathology|other")
    String reportType;

    @Length(max = 100)
    String reportName;

    @Length(max = 1000)
    String reportSummary;

    @Length(max = 10)
    String reportDate;

    @Length(max = 300)
    String doctorQuestion;

    @Size(max = 6)
    List<String> attachments;
}
