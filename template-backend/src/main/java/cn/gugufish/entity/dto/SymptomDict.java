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
@TableName("db_symptom_dict")
@NoArgsConstructor
@AllArgsConstructor
public class SymptomDict implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer bodyPartId;
    String name;
    String code;
    String keywords;
    String aliasKeywords;
    String description;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
