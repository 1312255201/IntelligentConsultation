package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.response.TriageMessageVO;
import cn.gugufish.entity.vo.response.TriageSessionVO;
import cn.gugufish.mapper.TriageMessageMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.TriageSessionQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TriageSessionQueryServiceImpl implements TriageSessionQueryService {

    @Resource
    TriageSessionMapper triageSessionMapper;

    @Resource
    TriageMessageMapper triageMessageMapper;

    @Override
    public TriageSessionVO detailByConsultationId(int consultationId) {
        TriageSession session = triageSessionMapper.selectOne(Wrappers.<TriageSession>query()
                .eq("consultation_id", consultationId));
        if (session == null) return null;

        List<TriageMessageVO> messages = triageMessageMapper.selectList(Wrappers.<TriageMessage>query()
                        .eq("session_id", session.getId())
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(TriageMessageVO.class))
                .toList();

        return session.asViewObject(TriageSessionVO.class, vo -> vo.setMessages(messages));
    }
}
