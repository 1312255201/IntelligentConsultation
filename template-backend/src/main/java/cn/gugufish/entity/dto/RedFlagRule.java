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
@TableName("db_red_flag_rule")
@NoArgsConstructor
@AllArgsConstructor
public class RedFlagRule implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String ruleName;
    String ruleCode;
    String triggerType;
    Integer bodyPartId;
    String keywordPattern;
    String conditionDescription;
    Integer triageLevelId;
    String suggestion;
    String actionType;
    Integer priority;
    Integer status;
    Date createTime;
    Date updateTime;
}
