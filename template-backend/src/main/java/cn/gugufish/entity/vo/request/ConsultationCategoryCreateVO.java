package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

@Data
public class ConsultationCategoryCreateVO {
    @NotNull
    @Positive
    Integer departmentId;
    @NotBlank
    @Length(max = 50)
    String name;
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    @Length(max = 50)
    String code;
    @Length(max = 255)
    String description;
    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    BigDecimal priceAmount;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
