package cn.gugufish.service;

import cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO;

public interface ConsultationDoctorHandleQueryService {
    ConsultationDoctorHandleVO detailByConsultationId(int consultationId);
}
