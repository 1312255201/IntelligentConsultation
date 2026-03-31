package cn.gugufish.service;

import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;

import java.util.List;

public interface DoctorWorkspaceService {
    DoctorWorkbenchVO workbench(int accountId);
    List<AdminConsultationRecordVO> consultationList(int accountId);
    AdminConsultationRecordVO consultationDetail(int accountId, int recordId);
    List<DoctorScheduleVO> scheduleList(int accountId);
}
