package cn.gugufish.service;

import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;

import java.util.List;

public interface ConsultationDoctorRecommendationService {
    List<ConsultationRecommendDoctorVO> recommendDoctors(ConsultationRecord record);
    List<ConsultationRecommendDoctorVO> recommendDoctors(ConsultationRecord record, ConsultationDispatchConfig config);
}
