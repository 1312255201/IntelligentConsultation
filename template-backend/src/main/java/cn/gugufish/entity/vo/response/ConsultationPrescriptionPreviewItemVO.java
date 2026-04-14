package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationPrescriptionPreviewItemVO {
    Integer medicineId;
    String medicineName;
    String specification;
    String warningSummary;
    List<String> warningDetails;
}
