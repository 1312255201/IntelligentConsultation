package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminDoctorMessageAiUsageOverviewVO {
    Integer generatedCount;
    Integer appliedCount;
    Integer sentCount;
    Integer templateUsedCount;
    Integer deepseekCount;
    Integer fallbackCount;
    Double applyRate;
    Double sendAdoptionRate;
    List<AdminDoctorMessageAiUsageSceneVO> sceneBreakdown;
    List<AdminDoctorMessageAiUsageItemVO> recentItems;
}
