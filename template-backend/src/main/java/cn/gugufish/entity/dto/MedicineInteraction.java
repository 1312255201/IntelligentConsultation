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
@TableName("db_medicine_interaction")
@NoArgsConstructor
@AllArgsConstructor
public class MedicineInteraction implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer medicineId;
    Integer conflictMedicineId;
    String interactionText;
    Integer status;
    Date createTime;
    Date updateTime;
}
