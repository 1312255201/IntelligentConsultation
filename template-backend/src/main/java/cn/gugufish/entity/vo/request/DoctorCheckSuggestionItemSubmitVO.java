package cn.gugufish.entity.vo.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DoctorCheckSuggestionItemSubmitVO {
    @Length(max = 100)
    String itemName;

    @Pattern(regexp = "lab|imaging|pathology|other")
    String itemType;

    @Pattern(regexp = "routine|soon|urgent")
    String urgencyLevel;

    @Length(max = 300)
    String purpose;

    @Length(max = 300)
    String attentionNote;
}
