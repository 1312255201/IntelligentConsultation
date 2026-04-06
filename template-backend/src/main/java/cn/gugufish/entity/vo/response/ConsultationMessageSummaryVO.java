package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationMessageSummaryVO {
    Integer totalCount;
    Integer userMessageCount;
    Integer doctorMessageCount;
    Integer unreadCount;
    String latestSenderType;
    String latestSenderName;
    String latestMessageType;
    String latestMessagePreview;
    Date latestTime;
}
