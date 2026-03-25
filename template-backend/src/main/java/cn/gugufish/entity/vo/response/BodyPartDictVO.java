package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class BodyPartDictVO {
    Integer id;
    String name;
    String code;
    Integer parentId;
    String description;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
