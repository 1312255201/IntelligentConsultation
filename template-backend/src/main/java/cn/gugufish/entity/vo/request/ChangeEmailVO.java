package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ChangeEmailVO {
    @NotBlank
    @Email
    String email;
    @NotBlank
    @Length(min = 6, max = 20)
    String password;
}
