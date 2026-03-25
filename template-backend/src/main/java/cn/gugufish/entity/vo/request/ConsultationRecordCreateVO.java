package cn.gugufish.entity.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ConsultationRecordCreateVO {
    @NotNull
    @Positive
    Integer patientId;
    @NotNull
    @Positive
    Integer categoryId;
    @NotNull
    @Positive
    Integer templateId;
    @Size(max = 100)
    List<@Valid ConsultationAnswerSubmitVO> answers;
}
