package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ConsultationTriageMessageSendVO {
    @NotNull
    @Positive
    Integer recordId;

    @NotBlank
    @Length(max = 1000)
    String content;
}
