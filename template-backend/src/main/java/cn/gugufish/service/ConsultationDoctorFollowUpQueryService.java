package cn.gugufish.service;

import cn.gugufish.entity.vo.response.ConsultationDoctorFollowUpVO;

import java.util.List;

public interface ConsultationDoctorFollowUpQueryService {
    List<ConsultationDoctorFollowUpVO> listByConsultationId(int consultationId);
}
