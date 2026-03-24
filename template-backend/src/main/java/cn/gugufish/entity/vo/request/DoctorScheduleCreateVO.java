package cn.gugufish.entity.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

@Data
public class DoctorScheduleCreateVO {
    @NotNull
    @Positive
    Integer doctorId;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date scheduleDate;
    @NotBlank
    @Pattern(regexp = "(morning|afternoon|evening|full_day)")
    String timePeriod;
    @NotBlank
    @Pattern(regexp = "(online|offline|both|followup|report)")
    String visitType;
    @NotNull
    @Min(0)
    @Max(999)
    Integer maxCapacity;
    @NotNull
    @Min(0)
    @Max(999)
    Integer usedCapacity;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
    @Length(max = 255)
    String remark;
}
