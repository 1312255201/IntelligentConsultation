package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.service.ConsultationDoctorAssignmentQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ConsultationDoctorAssignmentQueryServiceImpl implements ConsultationDoctorAssignmentQueryService {

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Override
    public ConsultationDoctorAssignmentVO detailByConsultationId(int consultationId) {
        ConsultationDoctorAssignment assignment = consultationDoctorAssignmentMapper.selectOne(Wrappers.<ConsultationDoctorAssignment>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("id")
                .last("limit 1"));
        return assignment == null ? null : assignment.asViewObject(ConsultationDoctorAssignmentVO.class);
    }
}
