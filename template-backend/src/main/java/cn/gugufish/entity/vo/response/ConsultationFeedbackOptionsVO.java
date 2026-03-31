package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationFeedbackOptionsVO {
    List<ConsultationFeedbackDepartmentOptionVO> departments;
    List<ConsultationFeedbackDoctorOptionVO> doctors;
}
