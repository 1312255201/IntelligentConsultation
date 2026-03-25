package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class ConsultationEntryCategoryVO {
    Integer id;
    Integer departmentId;
    String departmentName;
    String name;
    String code;
    String description;
    Integer sort;
    Integer defaultTemplateId;
    String defaultTemplateName;
    String defaultTemplateDescription;
    Integer defaultTemplateFieldCount;
}
