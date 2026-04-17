package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class AdminAccountPasswordResetVO {
    @NotNull
    @Positive
    Integer id;
    @NotBlank
    @Length(min = 6, max = 32)
    String password;
}
