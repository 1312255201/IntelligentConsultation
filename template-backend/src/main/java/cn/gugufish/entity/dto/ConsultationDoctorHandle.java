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
@TableName("db_consultation_doctor_handle")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDoctorHandle implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String status;
    String summary;
    String medicalAdvice;
    String followUpPlan;
    String internalRemark;
    Date receiveTime;
    Date completeTime;
    Date createTime;
    Date updateTime;
}
