package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.TriageFeedback;
import cn.gugufish.entity.vo.response.TriageFeedbackVO;
import cn.gugufish.mapper.TriageFeedbackMapper;
import cn.gugufish.service.TriageFeedbackQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class TriageFeedbackQueryServiceImpl implements TriageFeedbackQueryService {

    @Resource
    TriageFeedbackMapper triageFeedbackMapper;

    @Override
    public TriageFeedbackVO detailByConsultationId(int consultationId) {
        TriageFeedback feedback = triageFeedbackMapper.selectOne(Wrappers.<TriageFeedback>query()
                .eq("consultation_id", consultationId)
                .eq("status", 1)
                .orderByDesc("id")
                .last("limit 1"));
        return feedback == null ? null : feedback.asViewObject(TriageFeedbackVO.class);
    }
}
