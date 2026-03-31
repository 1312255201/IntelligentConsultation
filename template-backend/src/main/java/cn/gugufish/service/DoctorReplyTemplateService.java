package cn.gugufish.service;

import cn.gugufish.entity.dto.DoctorReplyTemplate;
import cn.gugufish.entity.vo.request.DoctorReplyTemplateCreateVO;
import cn.gugufish.entity.vo.request.DoctorReplyTemplateUpdateVO;

import java.util.List;

public interface DoctorReplyTemplateService {
    List<DoctorReplyTemplate> listDoctorReplyTemplates(int accountId);
    String createDoctorReplyTemplate(int accountId, DoctorReplyTemplateCreateVO vo);
    String updateDoctorReplyTemplate(int accountId, DoctorReplyTemplateUpdateVO vo);
    String deleteDoctorReplyTemplate(int accountId, int id);
}
