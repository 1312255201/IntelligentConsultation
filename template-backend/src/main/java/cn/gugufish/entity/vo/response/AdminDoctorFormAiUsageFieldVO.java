package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminDoctorFormAiUsageFieldVO {
    String fieldKey;
    String fieldLabel;
    String sceneType;
    String sceneLabel;
    Integer relatedCount;
    Integer regenerateCount;
    Integer templateComposeCount;
    Integer contextRewriteCount;
    Integer appliedCount;
    Integer savedCount;
    Integer contextRewriteSavedCount;
    Double applyRate;
    Double saveRate;
    Double contextRewriteSaveRate;
}
