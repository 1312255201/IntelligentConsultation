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
@TableName("db_triage_level_dict")
@NoArgsConstructor
@AllArgsConstructor
public class TriageLevelDict implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String name;
    String code;
    String description;
    String suggestion;
    String color;
    Integer priority;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
