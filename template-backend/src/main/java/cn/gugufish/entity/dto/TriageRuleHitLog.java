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
@TableName("db_triage_rule_hit_log")
@NoArgsConstructor
@AllArgsConstructor
public class TriageRuleHitLog implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer ruleId;
    String ruleName;
    String ruleCode;
    String triggerType;
    Integer triageLevelId;
    String triageLevelCode;
    String triageLevelName;
    String actionType;
    String suggestion;
    String matchedSummary;
    Integer priority;
    Integer isPrimary;
    Date createTime;
}
