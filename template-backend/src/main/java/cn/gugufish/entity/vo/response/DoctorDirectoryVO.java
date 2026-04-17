package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DoctorDirectoryVO {
    Integer id;
    Integer departmentId;
    String departmentName;
    String name;
    String title;
    String photo;
    String introduction;
    String expertise;
    Integer status;
    Integer availableScheduleCount;
    String nextScheduleText;
    Date updateTime;
    List<String> serviceTags;
}
