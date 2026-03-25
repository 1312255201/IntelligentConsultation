package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class RedFlagRuleCreateVO {
    @NotBlank
    @Length(max = 100)
    String ruleName;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 50)
    String ruleCode;
    @NotBlank
    @Pattern(regexp = "(symptom_match|keyword_match|body_part_match|combination)")
    String triggerType;
    @Positive
    Integer bodyPartId;
    @Length(max = 255)
    String keywordPattern;
    @Length(max = 255)
    String conditionDescription;
    @NotNull
    @Positive
    Integer triageLevelId;
    @NotBlank
    @Length(max = 255)
    String suggestion;
    @NotBlank
    @Pattern(regexp = "(emergency|offline|online|observe)")
    String actionType;
    @NotNull
    @Min(0)
    @Max(999)
    Integer priority;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
    @Size(max = 20)
    List<@Positive Integer> symptomIds;
}
