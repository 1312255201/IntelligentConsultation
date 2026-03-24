package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class HomepageCaseUpdateVO {
    @NotNull
    @Positive
    Integer id;
    @NotNull
    @Positive
    Integer departmentId;
    Integer doctorId;
    @NotBlank
    @Length(max = 100)
    String title;
    @NotBlank
    @Length(max = 191)
    String cover;
    @NotBlank
    @Length(max = 500)
    String summary;
    @Length(max = 3000)
    String detail;
    @Length(max = 255)
    String tags;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
