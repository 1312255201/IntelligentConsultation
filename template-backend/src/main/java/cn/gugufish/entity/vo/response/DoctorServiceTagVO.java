package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class DoctorServiceTagVO {
    Integer id;
    Integer doctorId;
    String tagCode;
    String tagName;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
