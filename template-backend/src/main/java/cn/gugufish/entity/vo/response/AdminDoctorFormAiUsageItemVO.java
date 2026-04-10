package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminDoctorFormAiUsageItemVO {
    Integer logId;
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    Integer doctorId;
    String doctorName;
    String departmentName;
    String sceneType;
    String regenerateField;
    String source;
    String promptVersion;
    Integer fallback;
    Integer draftContextUsed;
    String rewriteRequirement;
    Integer applyCount;
    String lastApplyMode;
    String lastApplyTarget;
    Integer templateUsed;
    String templateTitle;
    Integer savedStatus;
    String savedTarget;
    String draftSummary;
    String draftPreview;
    String savedPreview;
    Date generatedTime;
    Date lastApplyTime;
    Date savedTime;
}
