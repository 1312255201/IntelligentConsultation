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
public class BodyPartDictCreateVO {
    @NotBlank
    @Length(max = 50)
    String name;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 50)
    String code;
    @Positive
    Integer parentId;
    @Length(max = 255)
    String description;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
