package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationPayment;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.vo.response.ConsultationPaymentVO;
import cn.gugufish.mapper.ConsultationPaymentMapper;
import cn.gugufish.service.ConsultationPaymentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ConsultationPaymentServiceImpl extends ServiceImpl<ConsultationPaymentMapper, ConsultationPayment> implements ConsultationPaymentService {

    private static final SimpleDateFormat PAYMENT_NO_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    @Override
    public Map<Integer, ConsultationPaymentVO> mapByConsultationIds(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();
        Map<Integer, ConsultationPaymentVO> map = new HashMap<>();
        this.list(Wrappers.<ConsultationPayment>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> map.putIfAbsent(item.getConsultationId(), item.asViewObject(ConsultationPaymentVO.class)));
        return map;
    }

    @Override
    public ConsultationPaymentVO detailByConsultationId(int consultationId) {
        ConsultationPayment payment = this.getOne(Wrappers.<ConsultationPayment>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
        return payment == null ? null : payment.asViewObject(ConsultationPaymentVO.class);
    }

    @Override
    @Transactional
    public String createPendingPayment(ConsultationRecord record, BigDecimal amount, Date now) {
        if (record == null || record.getId() == null) return "闂瘖鏀惰垂蹇収鐢熸垚澶辫触";
        Date currentTime = now == null ? new Date() : now;
        BigDecimal normalizedAmount = normalizeAmount(amount);
        ConsultationPayment current = this.getOne(Wrappers.<ConsultationPayment>query()
                .eq("consultation_id", record.getId())
                .last("limit 1"));
        if (current == null) {
            ConsultationPayment payment = new ConsultationPayment(
                    null,
                    record.getId(),
                    record.getAccountId(),
                    record.getPatientId(),
                    record.getPatientName(),
                    record.getCategoryId(),
                    record.getCategoryName(),
                    normalizedAmount,
                    "pending",
                    generatePaymentNo(record.getId(), currentTime),
                    null,
                    null,
                    currentTime,
                    currentTime
            );
            return this.save(payment) ? null : "闂瘖鏀惰垂蹇収淇濆瓨澶辫触";
        }

        current.setAccountId(record.getAccountId());
        current.setPatientId(record.getPatientId());
        current.setPatientName(record.getPatientName());
        current.setCategoryId(record.getCategoryId());
        current.setCategoryName(record.getCategoryName());
        current.setAmount(normalizedAmount);
        if (trimToNull(current.getPaymentNo()) == null) {
            current.setPaymentNo(generatePaymentNo(record.getId(), currentTime));
        }
        current.setUpdateTime(currentTime);
        return this.updateById(current) ? null : "闂瘖鏀惰垂蹇収鏇存柊澶辫触";
    }

    @Override
    @Transactional
    public String mockPay(int accountId, int consultationId) {
        ConsultationPayment payment = this.getOne(Wrappers.<ConsultationPayment>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
        if (payment == null) return "褰撳墠闂瘖灏氭湭鐢熸垚鏀惰垂璁板綍";
        if (!Objects.equals(payment.getAccountId(), accountId)) return "褰撳墠鏀惰垂璁板綍涓嶅睘浜庝綘";
        if (Objects.equals(payment.getStatus(), "paid")) return null;

        Date now = new Date();
        payment.setStatus("paid");
        payment.setPaymentChannel("mock");
        payment.setPaidTime(now);
        if (trimToNull(payment.getPaymentNo()) == null) {
            payment.setPaymentNo(generatePaymentNo(consultationId, now));
        }
        payment.setUpdateTime(now);
        return this.updateById(payment) ? null : "妯℃嫙鏀粯澶辫触";
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            value = BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String generatePaymentNo(Integer consultationId, Date now) {
        String timePart = PAYMENT_NO_FORMAT.format(now == null ? new Date() : now);
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "PAY" + timePart + randomPart + (consultationId == null ? "" : consultationId);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
