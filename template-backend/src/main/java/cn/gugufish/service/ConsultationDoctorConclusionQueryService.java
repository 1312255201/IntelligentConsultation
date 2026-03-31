package cn.gugufish.service;

import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;

public interface ConsultationDoctorConclusionQueryService {
    ConsultationDoctorConclusionVO detailByConsultationId(int consultationId);
}
