package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ConsultationIntakeFieldSaveVO {
    Integer id;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 50)
    String fieldCode;
    @NotBlank
    @Length(max = 100)
    String fieldLabel;
    @NotBlank
    @Pattern(regexp = "(input|textarea|single_select|multi_select|date|number|upload|switch)")
    String fieldType;
    @NotNull
    @Min(0)
    @Max(1)
    Integer isRequired;
    @Length(max = 5000)
    String optionsJson;
    @Length(max = 255)
    String defaultValue;
    @Length(max = 255)
    String placeholder;
    @Length(max = 255)
    String helpText;
    @Length(max = 255)
    String conditionRule;
    @Length(max = 255)
    String validationRule;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
