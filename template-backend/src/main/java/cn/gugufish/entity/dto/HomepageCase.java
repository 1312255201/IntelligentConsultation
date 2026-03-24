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
@TableName("db_homepage_case")
@NoArgsConstructor
@AllArgsConstructor
public class HomepageCase implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer departmentId;
    Integer doctorId;
    String title;
    String cover;
    String summary;
    String detail;
    String tags;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
