package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class MedicineCreateVO {
    @NotBlank
    @Length(max = 100)
    String name;

    @Length(max = 100)
    String genericName;

    @Length(max = 50)
    String categoryName;

    @Length(max = 100)
    String specification;

    List<String> warningTexts;

    List<Integer> conflictMedicineIds;

    @Min(0)
    @Max(999)
    Integer sort;

    @Min(0)
    @Max(1)
    Integer status;
}
