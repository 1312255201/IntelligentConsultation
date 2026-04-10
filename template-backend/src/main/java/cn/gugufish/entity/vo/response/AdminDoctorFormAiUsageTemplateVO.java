package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminDoctorFormAiUsageTemplateVO {
    String fieldKey;
    String fieldLabel;
    String sceneType;
    String sceneLabel;
    String templateSceneType;
    Integer templateId;
    String templateTitle;
    String templateLabel;
    Integer composeCount;
    Integer contextRewriteCount;
    Integer appliedCount;
    Integer savedCount;
    Integer contextRewriteSavedCount;
    Double applyRate;
    Double saveRate;
    Double contextRewriteSaveRate;
}
