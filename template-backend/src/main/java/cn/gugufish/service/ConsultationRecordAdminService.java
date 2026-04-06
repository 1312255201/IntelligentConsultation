package cn.gugufish.service;

import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiSummaryVO;
import cn.gugufish.entity.vo.response.AdminConsultationDispatchSummaryVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiFieldSampleVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiMismatchVO;

import java.util.List;

public interface ConsultationRecordAdminService {
    List<AdminConsultationRecordVO> listRecords();
    AdminConsultationRecordVO recordDetail(int id);
    AdminConsultationAiSummaryVO aiSummary();
    AdminConsultationDispatchSummaryVO smartDispatchSummary();
    List<AdminConsultationAiMismatchVO> mismatchSamples(int limit,
                                                        int offset,
                                                        String keyword,
                                                        String doctorName,
                                                        String reasonCode,
                                                        String categoryName,
                                                        String departmentName);
    List<AdminConsultationAiFieldSampleVO> fieldSamples(String fieldKey,
                                                        String status,
                                                        int limit,
                                                        int offset,
                                                        String keyword,
                                                        String doctorName,
                                                        String categoryName,
                                                        String departmentName);
    byte[] exportMismatchSamplesCsv(String keyword,
                                    String doctorName,
                                    String reasonCode,
                                    String categoryName,
                                    String departmentName);
    byte[] exportFieldSamplesCsv(String fieldKey,
                                 String status,
                                 String keyword,
                                 String doctorName,
                                 String categoryName,
                                 String departmentName);
}
