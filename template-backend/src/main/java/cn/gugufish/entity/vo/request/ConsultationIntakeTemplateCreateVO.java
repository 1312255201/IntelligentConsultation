package cn.gugufish.entity.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class ConsultationIntakeTemplateCreateVO {
    @NotNull
    @Positive
    Integer categoryId;
    @NotBlank
    @Length(max = 100)
    String name;
    @Length(max = 255)
    String description;
    @NotNull
    @Min(1)
    @Max(999)
    Integer version;
    @NotNull
    @Min(0)
    @Max(1)
    Integer isDefault;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
    @NotNull
    @Valid
    @Size(min = 1, max = 50)
    List<ConsultationIntakeFieldSaveVO> fields;
}
