package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
public class ConsultationMessageSendVO {
    @NotNull
    @Positive
    Integer recordId;

    @Length(max = 2000)
    String content;

    @Size(max = 6)
    List<String> attachments;
}
