package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class DoctorAccountOptionVO {
    Integer id;
    String username;
    String email;
    Integer bindDoctorId;
    String bindDoctorName;
}
