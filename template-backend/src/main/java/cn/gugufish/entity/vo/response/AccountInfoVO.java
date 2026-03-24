package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AccountInfoVO {
    Integer id;
    String username;
    String email;
    String role;
    String avatar;
    Date registerTime;
}
