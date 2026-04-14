package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ConsultationPaymentVO {
    Integer id;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    String patientName;
    Integer categoryId;
    String categoryName;
    BigDecimal amount;
    String status;
    String paymentNo;
    String paymentChannel;
    Date paidTime;
    Date createTime;
    Date updateTime;
}
