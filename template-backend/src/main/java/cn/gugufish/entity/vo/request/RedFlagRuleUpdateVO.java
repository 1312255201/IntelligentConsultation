package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedFlagRuleUpdateVO extends RedFlagRuleCreateVO {
    @NotNull
    @Positive
    Integer id;
}
