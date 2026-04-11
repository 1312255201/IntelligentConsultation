package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationServiceFeedback;
import cn.gugufish.entity.vo.response.ConsultationServiceFeedbackVO;
import cn.gugufish.mapper.ConsultationServiceFeedbackMapper;
import cn.gugufish.service.ConsultationServiceFeedbackQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsultationServiceFeedbackQueryServiceImpl implements ConsultationServiceFeedbackQueryService {

    @Resource
    ConsultationServiceFeedbackMapper consultationServiceFeedbackMapper;

    @Override
    public ConsultationServiceFeedbackVO detailByConsultationId(int consultationId) {
        ConsultationServiceFeedback feedback = consultationServiceFeedbackMapper.selectOne(Wrappers.<ConsultationServiceFeedback>query()
                .eq("consultation_id", consultationId)
                .eq("status", 1)
                .orderByDesc("id")
                .last("limit 1"));
        return feedback == null ? null : feedback.asViewObject(ConsultationServiceFeedbackVO.class);
    }

    @Override
    public Map<Integer, ConsultationServiceFeedbackVO> mapByConsultationIds(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();

        Map<Integer, ConsultationServiceFeedbackVO> result = new HashMap<>();
        consultationServiceFeedbackMapper.selectList(Wrappers.<ConsultationServiceFeedback>query()
                        .in("consultation_id", consultationIds)
                        .eq("status", 1)
                        .orderByDesc("id"))
                .forEach(item -> result.putIfAbsent(item.getConsultationId(),
                        item.asViewObject(ConsultationServiceFeedbackVO.class)));
        return result;
    }

    @Override
    public List<ConsultationServiceFeedbackVO> listByDoctorId(int doctorId) {
        return consultationServiceFeedbackMapper.selectList(Wrappers.<ConsultationServiceFeedback>query()
                        .eq("doctor_id", doctorId)
                        .eq("status", 1)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationServiceFeedbackVO.class))
                .toList();
    }
}
