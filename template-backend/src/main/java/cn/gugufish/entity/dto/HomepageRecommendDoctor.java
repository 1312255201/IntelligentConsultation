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
@TableName("db_homepage_recommend_doctor")
@NoArgsConstructor
@AllArgsConstructor
public class HomepageRecommendDoctor implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer doctorId;
    String displayTitle;
    String recommendReason;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
