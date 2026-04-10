package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_doctor_form_ai_log")
@NoArgsConstructor
@AllArgsConstructor
public class DoctorFormAiLog implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    String consultationNo;
    String patientName;
    String categoryName;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String sceneType;
    String regenerateField;
    String source;
    String promptVersion;
    Integer fallback;
    String riskFlagsJson;
    String draftSummary;
    Integer draftContextUsed;
    String rewriteRequirement;
    String draftPreview;
    String draftPayloadJson;
    Integer applyCount;
    String lastApplyMode;
    String lastApplyTarget;
    String templateSceneType;
    Integer templateId;
    String templateTitle;
    Integer templateUsed;
    Integer savedStatus;
    String savedTarget;
    String savedPreview;
    String savedPayloadJson;
    Date generatedTime;
    Date lastApplyTime;
    Date savedTime;
    Date createTime;
    Date updateTime;
}
