package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class SymptomDictVO {
    Integer id;
    Integer bodyPartId;
    String name;
    String code;
    String keywords;
    String aliasKeywords;
    String description;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
