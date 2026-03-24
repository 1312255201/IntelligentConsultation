package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_homepage_config")
@NoArgsConstructor
@AllArgsConstructor
public class HomepageConfig implements BaseData {
    @TableId
    Integer id;
    String heroTitle;
    String heroSubtitle;
    String noticeText;
    String introTitle;
    String introContent;
    String servicePhone;
    Date updateTime;
}
