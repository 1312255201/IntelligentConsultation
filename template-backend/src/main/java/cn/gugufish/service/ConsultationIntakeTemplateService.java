package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationIntakeTemplateCreateVO;
import cn.gugufish.entity.vo.request.ConsultationIntakeTemplateUpdateVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;

import java.util.List;

public interface ConsultationIntakeTemplateService {
    List<ConsultationIntakeTemplateVO> listTemplates();
    ConsultationIntakeTemplateVO templateDetail(int id);
    String createTemplate(ConsultationIntakeTemplateCreateVO vo);
    String updateTemplate(ConsultationIntakeTemplateUpdateVO vo);
    String deleteTemplate(int id);
}
