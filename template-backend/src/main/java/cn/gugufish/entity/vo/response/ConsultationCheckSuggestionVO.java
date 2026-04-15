package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationCheckSuggestionVO {
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String itemName;
    String itemType;
    String urgencyLevel;
    String purpose;
    String attentionNote;
    Integer status;
    Integer sort;
    Date createTime;
    Date updateTime;
}
