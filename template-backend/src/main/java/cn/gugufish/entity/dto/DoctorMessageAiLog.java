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
@TableName("db_doctor_message_ai_log")
@NoArgsConstructor
@AllArgsConstructor
public class DoctorMessageAiLog implements BaseData {
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
    String source;
    String promptVersion;
    Integer fallback;
    String riskFlagsJson;
    String draftSummary;
    String draftContent;
    Integer applyCount;
    String lastApplyMode;
    String templateSceneType;
    Integer templateId;
    String templateTitle;
    Integer templateUsed;
    Integer sentStatus;
    Integer sentMessageId;
    String sentContentPreview;
    Date generatedTime;
    Date lastApplyTime;
    Date sentTime;
    Date createTime;
    Date updateTime;
}
