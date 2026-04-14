package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MedicineUpdateVO extends MedicineCreateVO {
    @Positive
    Integer id;
}
