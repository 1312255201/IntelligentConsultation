package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class HomepageCaseVO {
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
