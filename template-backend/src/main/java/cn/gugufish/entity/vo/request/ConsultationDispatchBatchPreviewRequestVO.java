package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConsultationDispatchBatchPreviewRequestVO extends ConsultationDispatchConfigSaveVO {
    @NotEmpty
    @Size(max = 12)
    List<@NotNull @Positive Integer> consultationIds;
}
