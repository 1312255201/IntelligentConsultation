package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class HomepageConfigSaveVO {
    @NotBlank
    @Length(max = 100)
    String heroTitle;
    @Length(max = 255)
    String heroSubtitle;
    @Length(max = 255)
    String noticeText;
    @Length(max = 100)
    String introTitle;
    @Length(max = 2000)
    String introContent;
    @Length(max = 30)
    String servicePhone;
}
