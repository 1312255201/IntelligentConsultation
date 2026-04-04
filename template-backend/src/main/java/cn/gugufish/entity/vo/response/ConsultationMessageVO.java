package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationMessageVO {
    Integer id;
    Integer consultationId;
    String senderType;
    Integer senderId;
    String senderName;
    String senderRoleName;
    String messageType;
    String content;
    String attachmentsJson;
    Integer status;
    Date createTime;
    Date updateTime;
    List<String> attachments;
}
