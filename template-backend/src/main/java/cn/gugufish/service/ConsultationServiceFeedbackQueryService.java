package cn.gugufish.service;

import cn.gugufish.entity.vo.response.ConsultationServiceFeedbackVO;

import java.util.List;
import java.util.Map;

public interface ConsultationServiceFeedbackQueryService {
    ConsultationServiceFeedbackVO detailByConsultationId(int consultationId);

    Map<Integer, ConsultationServiceFeedbackVO> mapByConsultationIds(List<Integer> consultationIds);

    List<ConsultationServiceFeedbackVO> listByDoctorId(int doctorId);
}
