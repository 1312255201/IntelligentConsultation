package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TriageRuleHitLogVO {
    Integer id;
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
