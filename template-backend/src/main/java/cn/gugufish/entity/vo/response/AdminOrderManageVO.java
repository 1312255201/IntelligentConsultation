package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AdminOrderManageVO {
    Integer id;
    String paymentNo;
    Integer consultationId;
    String consultationNo;
    String consultationStatus;
    Integer accountId;
    String username;
    Integer patientId;
    String patientName;
    String categoryName;
    String doctorName;
    String departmentName;
    BigDecimal amount;
    String status;
    String paymentChannel;
    Date paidTime;
    Date createTime;
    Date updateTime;
}
