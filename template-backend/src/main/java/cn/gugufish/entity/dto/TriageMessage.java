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
@TableName("db_triage_message")
@NoArgsConstructor
@AllArgsConstructor
public class TriageMessage implements BaseData {
    @TableId(type = IdType.AUTO)
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
