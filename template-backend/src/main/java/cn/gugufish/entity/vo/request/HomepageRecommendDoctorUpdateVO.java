package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class HomepageRecommendDoctorUpdateVO {
    @NotNull
    @Positive
    Integer id;
    @NotNull
    @Positive
    Integer doctorId;
    @Length(max = 100)
    String displayTitle;
    @Length(max = 255)
    String recommendReason;
    @NotNull
    @Min(0)
    @Max(999)
    Integer sort;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
