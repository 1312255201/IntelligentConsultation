package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("db_triage_result")
@NoArgsConstructor
@AllArgsConstructor
public class TriageResult implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer sessionId;
    Integer consultationId;
    String resultType;
    Integer triageLevelId;
    String triageLevelCode;
    String triageLevelName;
    Integer departmentId;
    String departmentName;
    Integer doctorId;
    String doctorName;
    String doctorCandidatesJson;
    String reasonText;
    String riskFlagsJson;
    String symptomExtractJson;
    BigDecimal confidenceScore;
    Integer isFinal;
    Integer status;
    Date createTime;
    Date updateTime;
}
