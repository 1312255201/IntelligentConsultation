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
@TableName("db_consultation_intake_template")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationIntakeTemplate implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer categoryId;
    String name;
    String description;
    Integer version;
    Integer isDefault;
    Integer status;
    Date createTime;
    Date updateTime;
}
