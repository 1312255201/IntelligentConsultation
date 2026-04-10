package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminDoctorFormAiUsageOverviewVO {
    Integer generatedCount;
    Integer fullGeneratedCount;
    Integer fieldRegenerateCount;
    Integer appliedCount;
    Integer pureAiAppliedCount;
    Integer templateUsedCount;
    Integer savedCount;
    Integer deepseekCount;
    Integer fallbackCount;
    Integer contextRewriteCount;
    Integer contextRewriteSavedCount;
    Double applyRate;
    Double saveAdoptionRate;
    Double contextRewriteSaveRate;
    List<AdminDoctorFormAiUsageSceneVO> sceneBreakdown;
    List<AdminDoctorFormAiUsagePromptVO> promptBreakdown;
    List<AdminDoctorFormAiUsageFieldVO> fieldBreakdown;
    List<AdminDoctorFormAiUsageTemplateVO> templateBreakdown;
    List<AdminDoctorFormAiUsageItemVO> recentItems;
}
