package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminDoctorFormAiUsagePromptVO {
    String promptVersion;
    Integer generatedCount;
    Integer fullGeneratedCount;
    Integer fieldRegenerateCount;
    Integer contextRewriteCount;
    Integer appliedCount;
    Integer savedCount;
    Double applyRate;
    Double saveRate;
}
