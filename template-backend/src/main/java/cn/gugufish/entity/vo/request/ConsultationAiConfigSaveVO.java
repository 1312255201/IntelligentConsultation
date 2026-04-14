package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationAiConfigSaveVO {
    @NotNull
    @Min(0)
    @Max(1)
    Integer enabled;
    @NotBlank
    @Size(max = 100)
    String promptVersion;
    @NotNull
    @Min(1)
    @Max(10)
    Integer doctorCandidateLimit;
}
