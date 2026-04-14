package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MedicineCatalogVO {
    Integer id;
    String name;
    String genericName;
    String categoryName;
    String specification;
    List<String> warningTexts;
    List<Integer> conflictMedicineIds;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
