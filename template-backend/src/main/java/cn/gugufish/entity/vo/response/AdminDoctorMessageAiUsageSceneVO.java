package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminDoctorMessageAiUsageSceneVO {
    String sceneType;
    String sceneLabel;
    Integer generatedCount;
    Integer appliedCount;
    Integer sentCount;
}
