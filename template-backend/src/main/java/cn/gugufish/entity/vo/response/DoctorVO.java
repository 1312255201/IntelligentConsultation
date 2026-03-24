package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class DoctorVO {
    Integer id;
    Integer departmentId;
    String name;
    String title;
    String photo;
    String introduction;
    String expertise;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
