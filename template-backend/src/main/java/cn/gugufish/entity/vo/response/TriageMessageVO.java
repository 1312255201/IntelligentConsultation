package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TriageMessageVO {
    Integer id;
    Integer sessionId;
    String roleType;
    String messageType;
    String title;
    String content;
    String structuredContent;
    Integer sort;
    Date createTime;
}
