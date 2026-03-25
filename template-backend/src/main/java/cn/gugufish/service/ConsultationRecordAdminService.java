package cn.gugufish.service;

import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;

import java.util.List;

public interface ConsultationRecordAdminService {
    List<AdminConsultationRecordVO> listRecords();
    AdminConsultationRecordVO recordDetail(int id);
}
