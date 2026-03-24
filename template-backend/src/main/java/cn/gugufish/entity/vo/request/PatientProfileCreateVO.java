package cn.gugufish.entity.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

@Data
public class PatientProfileCreateVO {
    @NotBlank
    @Length(max = 50)
    String name;
    @NotBlank
    @Pattern(regexp = "(male|female|unknown)")
    String gender;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date birthDate;
    @Length(max = 20)
    @Pattern(regexp = "(^$|^[0-9+\\-\\s]{6,20}$)")
    String phone;
    @Length(max = 30)
    String idCard;
    @NotBlank
    @Pattern(regexp = "(self|child|parent|spouse|other)")
    String relationType;
    @NotNull
    @Min(0)
    @Max(1)
    Integer isDefault;
    @Length(max = 255)
    String remark;
    @NotNull
    @Min(0)
    @Max(1)
    Integer status;
}
