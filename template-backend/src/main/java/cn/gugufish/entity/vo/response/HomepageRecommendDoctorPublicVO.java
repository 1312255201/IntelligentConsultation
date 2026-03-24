package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class HomepageRecommendDoctorPublicVO {
    Integer id;
    Integer doctorId;
    Integer departmentId;
    String departmentName;
    String name;
    String title;
    String photo;
    String introduction;
    String expertise;
    String displayTitle;
    String recommendReason;
    Integer sort;
}
