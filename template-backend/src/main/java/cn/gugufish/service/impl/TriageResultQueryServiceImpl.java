package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.vo.response.TriageResultVO;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.service.TriageResultQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TriageResultQueryServiceImpl implements TriageResultQueryService {

    @Resource
    TriageResultMapper triageResultMapper;

    @Override
    public TriageResultVO detailByConsultationId(int consultationId) {
        TriageResult result = triageResultMapper.selectOne(Wrappers.<TriageResult>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("is_final")
                .orderByDesc("id")
                .last("limit 1"));
        return result == null ? null : result.asViewObject(TriageResultVO.class);
    }
}
