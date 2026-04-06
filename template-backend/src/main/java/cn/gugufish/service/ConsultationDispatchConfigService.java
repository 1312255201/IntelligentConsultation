package cn.gugufish.service;

import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.vo.request.ConsultationDispatchConfigSaveVO;

public interface ConsultationDispatchConfigService {
    ConsultationDispatchConfig getConfig();
    String saveConfig(ConsultationDispatchConfigSaveVO vo);
}
