package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminOperationLogVO {
    Integer id;
    Long requestId;
    String requestUrl;
    String requestMethod;
    String remoteIp;
    Integer accountId;
    String username;
    String role;
    String requestParams;
    Integer responseCode;
    String responseSummary;
    Integer durationMs;
    Date createTime;
}
