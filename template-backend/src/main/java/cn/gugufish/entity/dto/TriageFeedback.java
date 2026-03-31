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
@TableName("db_triage_feedback")
@NoArgsConstructor
@AllArgsConstructor
public class TriageFeedback implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer sessionId;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    Integer userScore;
    Integer isAdopted;
    String feedbackText;
    Integer manualCorrectDepartmentId;
    String manualCorrectDepartmentName;
    Integer manualCorrectDoctorId;
    String manualCorrectDoctorName;
    Integer status;
    Date createTime;
    Date updateTime;
}
