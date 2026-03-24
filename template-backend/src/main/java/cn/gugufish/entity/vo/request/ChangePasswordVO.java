package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ChangePasswordVO {
    @NotBlank
    @Length(min = 6, max = 20)
    String oldPassword;
    @NotBlank
    @Length(min = 6, max = 20)
    String newPassword;
}
