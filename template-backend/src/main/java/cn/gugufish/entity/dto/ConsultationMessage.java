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
@TableName("db_consultation_message")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessage implements BaseData {
    @TableId(type = IdType.AUTO)
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
    Integer readStatus;
    Date readTime;
    Date createTime;
    Date updateTime;
}
