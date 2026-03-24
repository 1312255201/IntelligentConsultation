package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class HomepageConfigVO {
    Integer id;
    String heroTitle;
    String heroSubtitle;
    String noticeText;
    String introTitle;
    String introContent;
    String servicePhone;
    Date updateTime;
}
