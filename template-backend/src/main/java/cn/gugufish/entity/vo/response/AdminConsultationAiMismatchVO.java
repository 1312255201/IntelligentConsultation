package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AdminConsultationAiMismatchVO {
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String doctorName;
    String aiConditionLevel;
    String doctorConditionLevel;
    String aiDisposition;
    String doctorDisposition;
    String aiFollowUpText;
    String doctorFollowUpText;
    String aiReasonText;
    List<String> mismatchReasonCodes;
    String mismatchRemark;
    Date updateTime;
}
