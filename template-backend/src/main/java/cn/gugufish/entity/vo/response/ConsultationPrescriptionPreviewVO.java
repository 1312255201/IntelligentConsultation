package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationPrescriptionPreviewVO {
    Integer conflictDetected;
    List<String> overallWarnings;
    List<String> conflictWarnings;
    List<String> validationWarnings;
    List<ConsultationPrescriptionPreviewItemVO> items;
}
