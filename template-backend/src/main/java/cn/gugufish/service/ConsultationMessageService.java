package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;

import java.util.List;

public interface ConsultationMessageService {
    List<ConsultationMessageVO> listUserMessages(int accountId, int recordId);
    List<ConsultationMessageVO> listDoctorMessages(int accountId, int recordId);
    String sendUserMessage(int accountId, ConsultationMessageSendVO vo);
    String sendDoctorMessage(int accountId, ConsultationMessageSendVO vo);
}
