package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationRecordAnswerVO {
    Integer id;
    Integer consultationId;
    String fieldCode;
    String fieldLabel;
    String fieldType;
    String fieldValue;
    Integer sort;
    Date createTime;
    Date updateTime;
}
