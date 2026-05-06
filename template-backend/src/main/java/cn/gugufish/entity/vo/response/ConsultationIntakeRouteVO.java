package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConsultationIntakeRouteVO {
    String chiefComplaint;
    String matchMode;
    String matchModeLabel;
    String selectionMode;
    String selectionModeLabel;
    String routeReason;
    BigDecimal confidenceScore;
    ConsultationEntryCategoryVO category;
    ConsultationIntakeTemplateVO template;
}
