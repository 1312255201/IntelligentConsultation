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
@TableName("db_patient_medical_history")
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalHistory implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer patientId;
    String allergyHistory;
    String pastHistory;
    String chronicHistory;
    String surgeryHistory;
    String familyHistory;
    String medicationHistory;
    String pregnancyStatus;
    String lactationStatus;
    String infectiousHistory;
    Date createTime;
    Date updateTime;
}
