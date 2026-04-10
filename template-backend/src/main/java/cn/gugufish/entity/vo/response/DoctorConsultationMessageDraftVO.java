package cn.gugufish.entity.vo.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorConsultationMessageDraftVO {
    Integer logId;
    String content;
    String summary;
    List<String> riskFlags;
    String sceneType;
    String promptVersion;
    String source;
    Integer fallback;
}
