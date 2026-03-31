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
@TableName("db_consultation_doctor_follow_up")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDoctorFollowUp implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String followUpType;
    String patientStatus;
    String summary;
    String advice;
    String nextStep;
    Integer needRevisit;
    Date nextFollowUpDate;
    Date createTime;
    Date updateTime;
}
