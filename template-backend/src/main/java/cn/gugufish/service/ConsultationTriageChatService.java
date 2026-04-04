package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationTriageMessageSendVO;

public interface ConsultationTriageChatService {
    String sendUserMessage(int accountId, ConsultationTriageMessageSendVO vo);
}
