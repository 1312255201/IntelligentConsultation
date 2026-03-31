package cn.gugufish.service;

import cn.gugufish.entity.vo.response.TriageFeedbackVO;

public interface TriageFeedbackQueryService {
    TriageFeedbackVO detailByConsultationId(int consultationId);
}
