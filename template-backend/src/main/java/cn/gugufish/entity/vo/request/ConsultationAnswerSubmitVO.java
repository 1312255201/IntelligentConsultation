package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ConsultationAnswerSubmitVO {
    @NotBlank
    @Length(max = 50)
    String fieldCode;
    @Length(max = 5000)
    String fieldValue;
}
