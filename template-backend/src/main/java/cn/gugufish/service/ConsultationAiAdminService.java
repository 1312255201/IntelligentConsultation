package cn.gugufish.service;

import cn.gugufish.entity.vo.response.ConsultationAiAuditItemVO;
import cn.gugufish.entity.vo.response.ConsultationAiOverviewVO;
import cn.gugufish.entity.vo.response.AdminDoctorFormAiUsageOverviewVO;
import cn.gugufish.entity.vo.response.AdminDoctorMessageAiUsageOverviewVO;

import java.util.List;

public interface ConsultationAiAdminService {
    ConsultationAiOverviewVO overview();

    AdminDoctorMessageAiUsageOverviewVO doctorMessageUsageOverview();

    AdminDoctorFormAiUsageOverviewVO doctorFormUsageOverview();

    List<ConsultationAiAuditItemVO> auditList(String messageType, String keyword, boolean highRiskOnly, int limit);

    byte[] exportAuditListCsv(String messageType, String keyword, boolean highRiskOnly, int limit);

    List<ConsultationAiAuditItemVO> highRiskReviewQueue(String keyword, int limit);

    byte[] exportHighRiskReviewQueueCsv(String keyword, int limit);
}
