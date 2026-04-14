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
@TableName("db_medicine_warning")
@NoArgsConstructor
@AllArgsConstructor
public class MedicineWarning implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer medicineId;
    String warningText;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
