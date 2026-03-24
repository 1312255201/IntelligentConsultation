package cn.gugufish.entity.vo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PatientProfileVO {
    Integer id;
    String name;
    String gender;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date birthDate;
    String phone;
    String idCard;
    String relationType;
    Integer isSelf;
    Integer isDefault;
    String remark;
    Integer status;
    Date createTime;
    Date updateTime;
    Integer age;
}
