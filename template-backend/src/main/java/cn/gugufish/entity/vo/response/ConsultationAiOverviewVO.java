package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationAiOverviewVO {
    String providerName;
    String providerBaseUrl;
    String modelName;
    Double temperature;
    Integer maxTokens;
    String promptVersion;
    Integer doctorCandidateLimit;
    Boolean triageEnabled;
    Boolean providerEnabled;
    Boolean apiKeyConfigured;
    Boolean modelBeanReady;
    Boolean runtimeAvailable;
    String runtimeStatus;
    List<String> warnings;
    Long consultationCount;
    Long triageSessionCount;
    Long openSessionCount;
    Long triageResultCount;
    Long aiSummaryMessageCount;
    Long aiFollowupQuestionCount;
    Long aiChatReplyCount;
    Long userFollowupMessageCount;
    Date latestAiMessageTime;
}
