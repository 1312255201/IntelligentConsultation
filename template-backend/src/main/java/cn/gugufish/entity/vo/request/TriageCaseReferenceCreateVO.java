package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TriageCaseReferenceCreateVO {
    @NotBlank
    @Length(max = 100)
    String title;
    @NotBlank
    @Length(max = 500)
    String chiefComplaint;
    @Length(max = 5000)
    String symptomSummary;
    @NotBlank
    @Length(max = 500)
    String triageResult;
    @NotNull
    @Positive
    Integer departmentId;
    @Positive
    Integer doctorId;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 30)
    String riskLevel;
    @Length(max = 255)
    String tags;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 30)
    String sourceType;
    @Min(0)
    @Max(999)
    Integer sort;
    @Min(0)
    @Max(1)
    Integer status;
}
