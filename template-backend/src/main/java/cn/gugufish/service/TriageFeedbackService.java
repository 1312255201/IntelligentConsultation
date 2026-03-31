package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationTriageFeedbackSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackOptionsVO;

public interface TriageFeedbackService {
    ConsultationFeedbackOptionsVO feedbackOptions();
    String submitFeedback(int accountId, ConsultationTriageFeedbackSubmitVO vo);
}
