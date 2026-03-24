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
@TableName("db_consultation_category")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationCategory implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer departmentId;
    String name;
    String code;
    String description;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
