package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AdminAccountRoleUpdateVO {
    @NotNull
    @Positive
    Integer id;
    @NotBlank
    @Pattern(regexp = "^(user|doctor|admin)$")
    String role;
}
