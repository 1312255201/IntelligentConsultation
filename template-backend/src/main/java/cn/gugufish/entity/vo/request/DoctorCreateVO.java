package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorCreateVO {
    @NotNull
    @Positive
    Integer departmentId;
    @NotBlank
    @Length(max = 50)
    String name;
    @Length(max = 50)
    String title;
    @Length(max = 191)
    String photo;
    @Length(max = 2000)
    String introduction;
    @Length(max = 500)
    String expertise;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
