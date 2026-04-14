package cn.gugufish.service;

import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.vo.response.ConsultationPaymentVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ConsultationPaymentService {
    Map<Integer, ConsultationPaymentVO> mapByConsultationIds(List<Integer> consultationIds);
    ConsultationPaymentVO detailByConsultationId(int consultationId);
    String createPendingPayment(ConsultationRecord record, BigDecimal amount, Date now);
    String mockPay(int accountId, int consultationId);
}
