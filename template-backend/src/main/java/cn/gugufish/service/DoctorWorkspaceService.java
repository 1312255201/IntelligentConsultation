package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationPrescriptionPreviewRequestVO;
import cn.gugufish.entity.vo.request.DoctorConsultationAssignSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationAiDraftGenerateVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFollowUpSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFormDraftApplyVO;
import cn.gugufish.entity.vo.request.DoctorConsultationHandleSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationMessageDraftApplyVO;
import cn.gugufish.entity.vo.request.DoctorConsultationMessageDraftGenerateVO;
import cn.gugufish.entity.vo.request.DoctorConsultationServiceFeedbackHandleSubmitVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionPreviewVO;
import cn.gugufish.entity.vo.response.DoctorConsultationFollowUpDraftVO;
import cn.gugufish.entity.vo.response.DoctorConsultationHandleDraftVO;
import cn.gugufish.entity.vo.response.DoctorConsultationMessageDraftVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;
import cn.gugufish.entity.vo.response.MedicineCatalogVO;

import java.util.List;

public interface DoctorWorkspaceService {
    DoctorWorkbenchVO workbench(int accountId);
    List<AdminConsultationRecordVO> consultationList(int accountId);
    AdminConsultationRecordVO consultationDetail(int accountId, int recordId);
    DoctorConsultationHandleDraftVO generateConsultationHandleDraft(int accountId, DoctorConsultationAiDraftGenerateVO vo);
    DoctorConsultationFollowUpDraftVO generateConsultationFollowUpDraft(int accountId, DoctorConsultationAiDraftGenerateVO vo);
    String trackConsultationFormDraftApply(int accountId, DoctorConsultationFormDraftApplyVO vo);
    DoctorConsultationMessageDraftVO generateConsultationMessageDraft(int accountId, DoctorConsultationMessageDraftGenerateVO vo);
    String trackConsultationMessageDraftApply(int accountId, DoctorConsultationMessageDraftApplyVO vo);
    String claimConsultation(int accountId, DoctorConsultationAssignSubmitVO vo);
    String releaseConsultation(int accountId, DoctorConsultationAssignSubmitVO vo);
    String submitConsultationHandle(int accountId, DoctorConsultationHandleSubmitVO vo);
    List<MedicineCatalogVO> medicineOptions(int accountId);
    ConsultationPrescriptionPreviewVO previewConsultationPrescription(int accountId, ConsultationPrescriptionPreviewRequestVO vo);
    String submitConsultationFollowUp(int accountId, DoctorConsultationFollowUpSubmitVO vo);
    String submitConsultationServiceFeedbackHandle(int accountId, DoctorConsultationServiceFeedbackHandleSubmitVO vo);
    List<DoctorScheduleVO> scheduleList(int accountId);
}
