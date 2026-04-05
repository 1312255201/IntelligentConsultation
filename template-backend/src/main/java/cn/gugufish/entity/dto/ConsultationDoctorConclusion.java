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
@TableName("db_consultation_doctor_conclusion")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDoctorConclusion implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String conditionLevel;
    String disposition;
    String diagnosisDirection;
    String conclusionTagsJson;
    Integer needFollowUp;
    Integer followUpWithinDays;
    Integer isConsistentWithAi;
    String aiMismatchReasonsJson;
    String aiMismatchRemark;
    String patientInstruction;
    Date createTime;
    Date updateTime;
}
