package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TriageLevelDictCreateVO {
    @NotBlank
    @Length(max = 50)
    String name;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 50)
    String code;
    @Length(max = 255)
    String description;
    @Length(max = 255)
    String suggestion;
    @Length(max = 20)
    String color;
    @NotNull
    @Min(0)
    @Max(999)
    Integer priority;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
