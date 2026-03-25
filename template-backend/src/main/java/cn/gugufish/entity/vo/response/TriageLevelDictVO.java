package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TriageLevelDictVO {
    Integer id;
    String name;
    String code;
    String description;
    String suggestion;
    String color;
    Integer priority;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
