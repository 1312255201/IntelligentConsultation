package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AdminConsultationAiFieldSampleVO {
    String fieldKey;
    String fieldLabel;
    String compareStatus;
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String doctorName;
    String aiValueText;
    String doctorValueText;
    List<String> mismatchReasonCodes;
    String mismatchRemark;
    Date updateTime;
}
