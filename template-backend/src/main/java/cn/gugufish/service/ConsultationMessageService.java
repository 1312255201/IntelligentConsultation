package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
import cn.gugufish.entity.vo.response.ConsultationMessageSummaryVO;

import java.util.List;
import java.util.Map;

public interface ConsultationMessageService {
    List<ConsultationMessageVO> listUserMessages(int accountId, int recordId);
    List<ConsultationMessageVO> listDoctorMessages(int accountId, int recordId);
    String sendUserMessage(int accountId, ConsultationMessageSendVO vo);
    String sendDoctorMessage(int accountId, ConsultationMessageSendVO vo);
    Map<Integer, ConsultationMessageSummaryVO> summarizeDoctorMessages(List<Integer> consultationIds);
    Map<Integer, ConsultationMessageSummaryVO> summarizeUserMessages(List<Integer> consultationIds);
}
