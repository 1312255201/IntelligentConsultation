package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class HomepageCasePublicVO {
    Integer id;
    Integer departmentId;
    String departmentName;
    Integer doctorId;
    String doctorName;
    String doctorTitle;
    String title;
    String cover;
    String summary;
    String detail;
    String tags;
    Integer sort;
}
