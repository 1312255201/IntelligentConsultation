package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class DoctorReplyTemplateVO {
    Integer id;
    Integer doctorId;
    String sceneType;
    String title;
    String content;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
