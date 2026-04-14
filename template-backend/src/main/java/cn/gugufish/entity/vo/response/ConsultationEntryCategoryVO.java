package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsultationEntryCategoryVO {
    Integer id;
    Integer departmentId;
    String departmentName;
    String name;
    String code;
    String description;
    BigDecimal priceAmount;
    Integer sort;
    Integer defaultTemplateId;
    String defaultTemplateName;
    String defaultTemplateDescription;
    Integer defaultTemplateFieldCount;
}
