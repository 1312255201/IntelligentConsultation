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
@TableName("db_consultation_medication_feedback")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMedicationFeedback implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer prescriptionId;
    Integer medicineId;
    String medicineName;
    Integer accountId;
    Integer patientId;
    String patientName;
    String feedbackType;
    String severityLevel;
    String actionTaken;
    String feedbackSummary;
    String doctorQuestion;
    String attachmentsJson;
    Integer status;
    Date createTime;
    Date updateTime;
}
