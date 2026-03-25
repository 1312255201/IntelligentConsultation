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
@TableName("db_triage_knowledge")
@NoArgsConstructor
@AllArgsConstructor
public class TriageKnowledge implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String knowledgeType;
    String title;
    String content;
    String tags;
    Integer departmentId;
    Integer doctorId;
    String sourceType;
    Integer version;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
