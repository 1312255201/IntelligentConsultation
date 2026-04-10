package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminDoctorFormAiUsageSceneVO {
    String sceneType;
    String sceneLabel;
    Integer generatedCount;
    Integer fullGeneratedCount;
    Integer fieldRegenerateCount;
    Integer appliedCount;
    Integer pureAiAppliedCount;
    Integer templateUsedCount;
    Integer savedCount;
}
