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
public class DoctorServiceTagCreateVO {
    @NotNull
    @Positive
    Integer doctorId;
    @NotBlank
    @Length(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    String tagCode;
    @NotBlank
    @Length(max = 100)
    String tagName;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
