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
@TableName("db_consultation_service_feedback")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationServiceFeedback implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    String patientName;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    Integer serviceScore;
    Integer isResolved;
    String feedbackText;
    Integer doctorHandleStatus;
    String doctorHandleRemark;
    Integer doctorHandleAccountId;
    Integer doctorHandleDoctorId;
    String doctorHandleDoctorName;
    Date doctorHandleTime;
    Integer status;
    Date createTime;
    Date updateTime;
}
