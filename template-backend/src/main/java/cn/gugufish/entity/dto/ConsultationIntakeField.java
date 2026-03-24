package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_consultation_intake_field")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationIntakeField implements BaseData {
    @TableId(type = IdType.AUTO)
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
