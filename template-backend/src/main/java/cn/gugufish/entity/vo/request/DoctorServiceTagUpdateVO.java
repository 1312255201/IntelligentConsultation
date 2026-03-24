package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DoctorServiceTagUpdateVO extends DoctorServiceTagCreateVO {
    @NotNull
    @Positive
    Integer id;
}
