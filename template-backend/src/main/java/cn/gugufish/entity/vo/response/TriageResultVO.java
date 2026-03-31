package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TriageResultVO {
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
