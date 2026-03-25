package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RedFlagRuleVO {
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
    List<Integer> symptomIds;
}
