package cn.gugufish.service;

import cn.gugufish.entity.vo.request.DoctorConsultationAssignSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationHandleSubmitVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;

import java.util.List;

public interface DoctorWorkspaceService {
    DoctorWorkbenchVO workbench(int accountId);
    List<AdminConsultationRecordVO> consultationList(int accountId);
    AdminConsultationRecordVO consultationDetail(int accountId, int recordId);
    String claimConsultation(int accountId, DoctorConsultationAssignSubmitVO vo);
    String releaseConsultation(int accountId, DoctorConsultationAssignSubmitVO vo);
    String submitConsultationHandle(int accountId, DoctorConsultationHandleSubmitVO vo);
    List<DoctorScheduleVO> scheduleList(int accountId);
}
