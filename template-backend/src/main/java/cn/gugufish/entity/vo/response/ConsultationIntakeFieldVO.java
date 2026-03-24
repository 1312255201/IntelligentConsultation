package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationIntakeFieldVO {
    Integer id;
    Integer templateId;
    String fieldCode;
    String fieldLabel;
    String fieldType;
    Integer isRequired;
    String optionsJson;
    String defaultValue;
    String placeholder;
    String helpText;
    String conditionRule;
    String validationRule;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
