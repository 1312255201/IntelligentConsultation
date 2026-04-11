package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationServiceFeedback;
import cn.gugufish.entity.vo.request.ConsultationServiceFeedbackSubmitVO;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.ConsultationServiceFeedbackMapper;
import cn.gugufish.service.ConsultationServiceFeedbackService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ConsultationServiceFeedbackServiceImpl implements ConsultationServiceFeedbackService {

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationDoctorHandleMapper consultationDoctorHandleMapper;

    @Resource
    ConsultationServiceFeedbackMapper consultationServiceFeedbackMapper;

    @Override
    public String submitFeedback(int accountId, ConsultationServiceFeedbackSubmitVO vo) {
        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", vo.getRecordId())
                .eq("account_id", accountId));
        if (record == null) return "问诊记录不存在";
        if (!"completed".equals(record.getStatus())) return "仅已完成的问诊单可提交服务评价";

        ConsultationDoctorHandle handle = consultationDoctorHandleMapper.selectOne(Wrappers.<ConsultationDoctorHandle>query()
                .eq("consultation_id", record.getId())
                .orderByDesc("update_time")
                .orderByDesc("id")
                .last("limit 1"));
        if (handle == null || !"completed".equals(handle.getStatus())) return "当前问诊尚未完成医生处理";
        if (handle.getDoctorId() == null || blankToNull(handle.getDoctorName()) == null) return "当前问诊缺少有效医生处理信息，暂不可提交评价";

        Date now = new Date();
        ConsultationServiceFeedback current = consultationServiceFeedbackMapper.selectOne(Wrappers.<ConsultationServiceFeedback>query()
                .eq("consultation_id", record.getId())
                .eq("account_id", accountId)
                .eq("status", 1)
                .orderByDesc("id")
                .last("limit 1"));

        if (current == null) {
            ConsultationServiceFeedback feedback = new ConsultationServiceFeedback(
                    null,
                    record.getId(),
                    accountId,
                    record.getPatientId(),
                    record.getPatientName(),
                    handle.getDoctorId(),
                    handle.getDoctorName(),
                    handle.getDepartmentId(),
                    handle.getDepartmentName(),
                    vo.getServiceScore(),
                    vo.getIsResolved(),
                    blankToNull(vo.getFeedbackText()),
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1,
                    now,
                    now
            );
            return consultationServiceFeedbackMapper.insert(feedback) > 0 ? null : "问诊服务评价提交失败，请稍后重试";
        }

        boolean updated = consultationServiceFeedbackMapper.update(null, Wrappers.<ConsultationServiceFeedback>update()
                .eq("id", current.getId())
                .set("patient_id", record.getPatientId())
                .set("patient_name", record.getPatientName())
                .set("doctor_id", handle.getDoctorId())
                .set("doctor_name", handle.getDoctorName())
                .set("department_id", handle.getDepartmentId())
                .set("department_name", handle.getDepartmentName())
                .set("service_score", vo.getServiceScore())
                .set("is_resolved", vo.getIsResolved())
                .set("feedback_text", blankToNull(vo.getFeedbackText()))
                .set("doctor_handle_status", 0)
                .set("doctor_handle_remark", null)
                .set("doctor_handle_account_id", null)
                .set("doctor_handle_doctor_id", null)
                .set("doctor_handle_doctor_name", null)
                .set("doctor_handle_time", null)
                .set("status", 1)
                .set("update_time", now)) > 0;
        return updated ? null : "问诊服务评价更新失败，请稍后重试";
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
