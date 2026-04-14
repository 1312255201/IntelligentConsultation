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
@TableName("db_consultation_prescription")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPrescription implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer doctorId;
    Integer medicineId;
    String medicineName;
    String genericName;
    String categoryName;
    String specification;
    String dosage;
    String frequency;
    Integer durationDays;
    String medicationInstruction;
    String warningSummary;
    Integer sort;
    Date createTime;
    Date updateTime;
}
