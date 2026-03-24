package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationIntakeTemplateVO {
    Integer id;
    Integer categoryId;
    String name;
    String description;
    Integer version;
    Integer isDefault;
    Integer status;
    Integer fieldCount;
    Date createTime;
    Date updateTime;
    List<ConsultationIntakeFieldVO> fields;
}
