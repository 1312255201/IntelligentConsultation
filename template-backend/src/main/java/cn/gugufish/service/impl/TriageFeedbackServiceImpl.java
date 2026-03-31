package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.dto.TriageFeedback;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.request.ConsultationTriageFeedbackSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackDepartmentOptionVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackDoctorOptionVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackOptionsVO;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.PatientProfileMapper;
import cn.gugufish.mapper.TriageFeedbackMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.TriageFeedbackService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TriageFeedbackServiceImpl implements TriageFeedbackService {

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    PatientProfileMapper patientProfileMapper;

    @Resource
    TriageSessionMapper triageSessionMapper;

    @Resource
    TriageFeedbackMapper triageFeedbackMapper;

    @Override
    public ConsultationFeedbackOptionsVO feedbackOptions() {
        List<ConsultationFeedbackDepartmentOptionVO> departments = departmentMapper.selectList(Wrappers.<Department>query()
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationFeedbackDepartmentOptionVO.class))
                .toList();

        List<ConsultationFeedbackDoctorOptionVO> doctors = doctorMapper.selectList(Wrappers.<Doctor>query()
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationFeedbackDoctorOptionVO.class))
                .toList();

        ConsultationFeedbackOptionsVO vo = new ConsultationFeedbackOptionsVO();
        vo.setDepartments(departments);
        vo.setDoctors(doctors);
        return vo;
    }

    @Override
    public String submitFeedback(int accountId, ConsultationTriageFeedbackSubmitVO vo) {
        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", vo.getRecordId())
                .eq("account_id", accountId));
        if (record == null) return "问诊记录不存在";

        PatientProfile patient = patientProfileMapper.selectById(record.getPatientId());
        if (patient == null) return "就诊人不存在";

        TriageSession session = triageSessionMapper.selectOne(Wrappers.<TriageSession>query()
                .eq("consultation_id", record.getId()));
        if (session == null) return "当前问诊记录尚未生成导诊会话";

        Doctor manualDoctor = null;
        Integer departmentId = vo.getManualCorrectDepartmentId();
        String departmentName = null;
        if (vo.getManualCorrectDoctorId() != null) {
            manualDoctor = doctorMapper.selectById(vo.getManualCorrectDoctorId());
            if (manualDoctor == null || (manualDoctor.getStatus() != null && manualDoctor.getStatus() != 1)) {
                return "修正医生不存在或已停用";
            }
            if (departmentId == null) departmentId = manualDoctor.getDepartmentId();
        }

        if (departmentId != null) {
            Department department = departmentMapper.selectById(departmentId);
            if (department == null || (department.getStatus() != null && department.getStatus() != 1)) {
                return "修正科室不存在或已停用";
            }
            departmentName = department.getName();
            if (manualDoctor != null && manualDoctor.getDepartmentId() != null && !manualDoctor.getDepartmentId().equals(departmentId)) {
                return "修正医生与科室不匹配";
            }
        }

        Date now = new Date();
        TriageFeedback current = triageFeedbackMapper.selectOne(Wrappers.<TriageFeedback>query()
                .eq("consultation_id", record.getId())
                .last("limit 1"));

        String doctorName = manualDoctor == null ? null : manualDoctor.getName();
        if (current == null) {
            TriageFeedback feedback = new TriageFeedback(
                    null,
                    session.getId(),
                    record.getId(),
                    accountId,
                    patient.getId(),
                    vo.getUserScore(),
                    vo.getIsAdopted(),
                    blankToNull(vo.getFeedbackText()),
                    departmentId,
                    departmentName,
                    manualDoctor == null ? null : manualDoctor.getId(),
                    doctorName,
                    1,
                    now,
                    now
            );
            return triageFeedbackMapper.insert(feedback) > 0 ? null : "导诊反馈提交失败，请稍后重试";
        }

        boolean updated = triageFeedbackMapper.update(null, Wrappers.<TriageFeedback>update()
                .eq("id", current.getId())
                .set("session_id", session.getId())
                .set("user_score", vo.getUserScore())
                .set("is_adopted", vo.getIsAdopted())
                .set("feedback_text", blankToNull(vo.getFeedbackText()))
                .set("manual_correct_department_id", departmentId)
                .set("manual_correct_department_name", departmentName)
                .set("manual_correct_doctor_id", manualDoctor == null ? null : manualDoctor.getId())
                .set("manual_correct_doctor_name", doctorName)
                .set("status", 1)
                .set("update_time", now)) > 0;
        return updated ? null : "导诊反馈更新失败，请稍后重试";
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
