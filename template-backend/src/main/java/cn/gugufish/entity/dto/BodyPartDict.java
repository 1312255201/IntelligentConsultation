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
@TableName("db_body_part_dict")
@NoArgsConstructor
@AllArgsConstructor
public class BodyPartDict implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String name;
    String code;
    Integer parentId;
    String description;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
