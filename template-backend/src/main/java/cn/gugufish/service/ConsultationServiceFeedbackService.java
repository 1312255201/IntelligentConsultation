package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationServiceFeedbackSubmitVO;

public interface ConsultationServiceFeedbackService {
    String submitFeedback(int accountId, ConsultationServiceFeedbackSubmitVO vo);
}
