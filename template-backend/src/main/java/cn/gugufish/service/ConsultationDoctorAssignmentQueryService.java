package cn.gugufish.service;

import cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO;

public interface ConsultationDoctorAssignmentQueryService {
    ConsultationDoctorAssignmentVO detailByConsultationId(int consultationId);
}
