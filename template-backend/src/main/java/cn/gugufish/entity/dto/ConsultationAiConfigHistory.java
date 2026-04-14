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
@TableName("db_consultation_ai_config_history")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationAiConfigHistory implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer configId;
    Integer enabledBefore;
    Integer enabledAfter;
    String promptVersionBefore;
    String promptVersionAfter;
    Integer doctorCandidateLimitBefore;
    Integer doctorCandidateLimitAfter;
    Integer operatorAccountId;
    String operatorUsername;
    String changeSummary;
    Date createTime;
}
