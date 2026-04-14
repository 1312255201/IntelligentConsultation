package cn.gugufish.service;

import cn.gugufish.entity.dto.ConsultationAiConfig;
import cn.gugufish.entity.vo.response.ConsultationAiConfigHistoryVO;
import cn.gugufish.entity.vo.request.ConsultationAiConfigSaveVO;

import java.util.List;

public interface ConsultationAiConfigService {
    ConsultationAiConfig getConfig();

    String saveConfig(int operatorAccountId, ConsultationAiConfigSaveVO vo);

    List<ConsultationAiConfigHistoryVO> listHistory(int limit);
}
