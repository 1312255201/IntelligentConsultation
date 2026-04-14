package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationRecordCreateVO;
import cn.gugufish.entity.vo.request.ConsultationServiceFeedbackSubmitVO;
import cn.gugufish.entity.vo.request.ConsultationTriageFeedbackSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackOptionsVO;
import cn.gugufish.entity.vo.response.ConsultationEntryCategoryVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.entity.vo.response.ConsultationRecordVO;

import java.util.List;

public interface ConsultationService {
    List<ConsultationEntryCategoryVO> listEntryCategories();
    ConsultationIntakeTemplateVO defaultTemplateDetail(int categoryId);
    List<ConsultationRecordVO> listRecords(int accountId);
    ConsultationRecordVO recordDetail(int accountId, int id);
    String createRecord(int accountId, ConsultationRecordCreateVO vo);
    String mockPay(int accountId, int recordId);
    ConsultationFeedbackOptionsVO feedbackOptions();
    String submitTriageFeedback(int accountId, ConsultationTriageFeedbackSubmitVO vo);
    String submitServiceFeedback(int accountId, ConsultationServiceFeedbackSubmitVO vo);
}
