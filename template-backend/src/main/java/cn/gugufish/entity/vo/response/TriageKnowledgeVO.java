package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TriageKnowledgeVO {
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
