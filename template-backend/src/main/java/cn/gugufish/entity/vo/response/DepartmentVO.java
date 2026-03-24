package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class DepartmentVO {
    Integer id;
    String name;
    String code;
    String description;
    String location;
    String phone;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
