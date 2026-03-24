package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationCategoryVO {
    Integer id;
    Integer departmentId;
    String name;
    String code;
    String description;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
