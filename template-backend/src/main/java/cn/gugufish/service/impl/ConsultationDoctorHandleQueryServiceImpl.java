package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ConsultationDoctorHandleQueryServiceImpl implements ConsultationDoctorHandleQueryService {

    @Resource
    ConsultationDoctorHandleMapper consultationDoctorHandleMapper;

    @Override
    public ConsultationDoctorHandleVO detailByConsultationId(int consultationId) {
        ConsultationDoctorHandle handle = consultationDoctorHandleMapper.selectOne(Wrappers.<ConsultationDoctorHandle>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("id")
                .last("limit 1"));
        return handle == null ? null : handle.asViewObject(ConsultationDoctorHandleVO.class);
    }
}
