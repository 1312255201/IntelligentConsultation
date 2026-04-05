package cn.gugufish.entity.vo.response;

import lombok.Data;

@Data
public class AdminConsultationAiReasonVO {
    String reasonCode;
    String reasonLabel;
    Integer count;
}
