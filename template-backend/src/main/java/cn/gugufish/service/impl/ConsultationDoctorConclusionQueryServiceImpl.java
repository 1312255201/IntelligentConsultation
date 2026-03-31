package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ConsultationDoctorConclusionQueryServiceImpl implements ConsultationDoctorConclusionQueryService {

    @Resource
    ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper;

    @Override
    public ConsultationDoctorConclusionVO detailByConsultationId(int consultationId) {
        ConsultationDoctorConclusion conclusion = consultationDoctorConclusionMapper.selectOne(Wrappers.<ConsultationDoctorConclusion>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("id")
                .last("limit 1"));
        return conclusion == null ? null : conclusion.asViewObject(ConsultationDoctorConclusionVO.class);
    }
}
