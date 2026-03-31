package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorFollowUp;
import cn.gugufish.entity.vo.response.ConsultationDoctorFollowUpVO;
import cn.gugufish.mapper.ConsultationDoctorFollowUpMapper;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsultationDoctorFollowUpQueryServiceImpl implements ConsultationDoctorFollowUpQueryService {

    @Resource
    ConsultationDoctorFollowUpMapper consultationDoctorFollowUpMapper;

    @Override
    public List<ConsultationDoctorFollowUpVO> listByConsultationId(int consultationId) {
        return consultationDoctorFollowUpMapper.selectList(Wrappers.<ConsultationDoctorFollowUp>query()
                        .eq("consultation_id", consultationId)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationDoctorFollowUpVO.class))
                .toList();
    }
}
