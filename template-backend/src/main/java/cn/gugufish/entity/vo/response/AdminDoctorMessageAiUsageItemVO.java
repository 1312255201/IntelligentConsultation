package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminDoctorMessageAiUsageItemVO {
    Integer logId;
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    Integer doctorId;
    String doctorName;
    String departmentName;
    String sceneType;
    String source;
    Integer fallback;
    Integer applyCount;
    String lastApplyMode;
    Integer templateUsed;
    String templateTitle;
    Integer sentStatus;
    String draftSummary;
    String draftContent;
    String sentContentPreview;
    Date generatedTime;
    Date lastApplyTime;
    Date sentTime;
}
