package cn.gugufish.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("db_red_flag_rule_symptom")
@NoArgsConstructor
@AllArgsConstructor
public class RedFlagRuleSymptom {
    Integer ruleId;
    Integer symptomId;
}
