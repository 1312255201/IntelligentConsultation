package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class HomepageRecommendDoctorVO {
    Integer id;
    Integer doctorId;
    String displayTitle;
    String recommendReason;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
