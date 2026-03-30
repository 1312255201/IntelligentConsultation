package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TriageSessionVO {
    Integer id;
    String sessionNo;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    String patientName;
    Integer categoryId;
    String categoryName;
    Integer departmentId;
    String departmentName;
    String sourceType;
    String status;
    Integer triageLevelId;
    String triageLevelCode;
    String triageLevelName;
    String triageActionType;
    String triageSummary;
    Integer messageCount;
    Date startedTime;
    Date endedTime;
    Date createTime;
    Date updateTime;
    List<TriageMessageVO> messages;
}
