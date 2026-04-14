package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationAiConfigHistoryVO {
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
