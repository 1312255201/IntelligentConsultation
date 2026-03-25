package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TriageKnowledgeCreateVO {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 30)
    String knowledgeType;
    @NotBlank
    @Length(max = 100)
    String title;
    @NotBlank
    @Length(max = 10000)
    String content;
    @Length(max = 255)
    String tags;
    @Positive
    Integer departmentId;
    @Positive
    Integer doctorId;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 30)
    String sourceType;
    @Min(1)
    @Max(99)
    Integer version;
    @Min(0)
    @Max(999)
    Integer sort;
    @Min(0)
    @Max(1)
    Integer status;
}
