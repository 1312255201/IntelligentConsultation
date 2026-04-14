package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("db_consultation_payment")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPayment implements BaseData {
    @TableId(type = IdType.AUTO)
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
