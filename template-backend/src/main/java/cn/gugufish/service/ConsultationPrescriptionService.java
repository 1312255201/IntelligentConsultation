package cn.gugufish.service;

import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.vo.request.DoctorPrescriptionItemSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionPreviewVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionVO;

import java.util.Date;
import java.util.List;

public interface ConsultationPrescriptionService {
    List<ConsultationPrescriptionVO> listByConsultationId(int consultationId);
    ConsultationPrescriptionPreviewVO preview(List<DoctorPrescriptionItemSubmitVO> items);
    String replaceConsultationPrescriptions(int consultationId, Doctor doctor, List<DoctorPrescriptionItemSubmitVO> items, Date now);
}
