package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminAccountManageVO {
    Integer id;
    String username;
    String email;
    String role;
    String avatar;
    Date registerTime;
    Integer doctorId;
    String doctorName;
    Integer patientCount;
    Integer consultationCount;
    Integer paymentCount;
}
