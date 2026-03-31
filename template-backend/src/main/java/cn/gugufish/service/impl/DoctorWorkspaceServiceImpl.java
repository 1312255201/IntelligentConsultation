package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;
import cn.gugufish.entity.vo.response.TriageRuleHitLogVO;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.service.DoctorWorkspaceService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class DoctorWorkspaceServiceImpl implements DoctorWorkspaceService {

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    DoctorServiceTagMapper doctorServiceTagMapper;

    @Resource
    DoctorScheduleMapper doctorScheduleMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationRecordAnswerMapper consultationRecordAnswerMapper;

    @Resource
    TriageRuleHitLogMapper triageRuleHitLogMapper;

    @Resource
    TriageSessionQueryService triageSessionQueryService;

    @Resource
    TriageResultQueryService triageResultQueryService;

    @Resource
    TriageFeedbackQueryService triageFeedbackQueryService;

    @Override
    public DoctorWorkbenchVO workbench(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) {
            DoctorWorkbenchVO vo = new DoctorWorkbenchVO();
            vo.setBound(0);
            vo.setBindingMessage("当前 doctor 账号尚未绑定医生档案，请先由管理员在医生信息维护页面完成绑定");
            vo.setConsultationCount(0);
            vo.setTodayConsultationCount(0);
            vo.setRiskConsultationCount(0);
            vo.setUpcomingScheduleCount(0);
            vo.setServiceTagCount(0);
            vo.setServiceTags(List.of());
            vo.setRecentConsultations(List.of());
            vo.setUpcomingSchedules(List.of());
            return vo;
        }

        Department department = doctor.getDepartmentId() == null ? null : departmentMapper.selectById(doctor.getDepartmentId());
        List<String> serviceTags = doctorServiceTagMapper.selectList(Wrappers.<DoctorServiceTag>query()
                        .eq("doctor_id", doctor.getId())
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(DoctorServiceTag::getTagName)
                .toList();

        List<DoctorScheduleVO> schedules = scheduleList(accountId);
        List<AdminConsultationRecordVO> consultations = consultationList(accountId);

        LocalDate today = LocalDate.now();
        int todayCount = (int) consultations.stream()
                .filter(item -> isSameDay(item.getCreateTime(), today))
                .count();
        int riskCount = (int) consultations.stream()
                .filter(this::isRiskConsultation)
                .count();

        DoctorWorkbenchVO vo = new DoctorWorkbenchVO();
        vo.setBound(1);
        vo.setBindingMessage(doctor.getStatus() != null && doctor.getStatus() == 1
                ? "已完成医生账号绑定，可以开始使用医生工作台"
                : "当前医生档案已停用，如需接诊请先联系管理员启用");
        vo.setDoctorId(doctor.getId());
        vo.setDoctorStatus(doctor.getStatus());
        vo.setAccountId(doctor.getAccountId());
        vo.setDepartmentId(doctor.getDepartmentId());
        vo.setDepartmentName(department == null ? null : department.getName());
        vo.setDoctorName(doctor.getName());
        vo.setDoctorTitle(doctor.getTitle());
        vo.setPhoto(doctor.getPhoto());
        vo.setIntroduction(doctor.getIntroduction());
        vo.setExpertise(doctor.getExpertise());
        vo.setConsultationCount(consultations.size());
        vo.setTodayConsultationCount(todayCount);
        vo.setRiskConsultationCount(riskCount);
        vo.setUpcomingScheduleCount(schedules.size());
        vo.setServiceTagCount(serviceTags.size());
        vo.setServiceTags(serviceTags);
        vo.setRecentConsultations(consultations.stream().limit(6).toList());
        vo.setUpcomingSchedules(schedules.stream().limit(6).toList());
        return vo;
    }

    @Override
    public List<AdminConsultationRecordVO> consultationList(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null || doctor.getDepartmentId() == null) return List.of();

        return consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                        .eq("department_id", doctor.getDepartmentId())
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(AdminConsultationRecordVO.class))
                .toList();
    }

    @Override
    public AdminConsultationRecordVO consultationDetail(int accountId, int recordId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null || doctor.getDepartmentId() == null) return null;

        ConsultationRecord record = consultationRecordMapper.selectById(recordId);
        if (record == null || !doctor.getDepartmentId().equals(record.getDepartmentId())) return null;

        List<ConsultationRecordAnswerVO> answers = consultationRecordAnswerMapper.selectList(Wrappers.<ConsultationRecordAnswer>query()
                        .eq("consultation_id", recordId)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationRecordAnswerVO.class))
                .toList();

        List<TriageRuleHitLogVO> ruleHits = triageRuleHitLogMapper.selectList(Wrappers.<TriageRuleHitLog>query()
                        .eq("consultation_id", recordId)
                        .orderByDesc("is_primary")
                        .orderByDesc("priority")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(TriageRuleHitLogVO.class))
                .toList();

        return record.asViewObject(AdminConsultationRecordVO.class, vo -> {
            vo.setAnswers(answers);
            vo.setRuleHits(ruleHits);
            vo.setTriageSession(triageSessionQueryService.detailByConsultationId(recordId));
            vo.setTriageResult(triageResultQueryService.detailByConsultationId(recordId));
            vo.setTriageFeedback(triageFeedbackQueryService.detailByConsultationId(recordId));
        });
    }

    @Override
    public List<DoctorScheduleVO> scheduleList(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return List.of();

        Date today = java.sql.Date.valueOf(LocalDate.now());
        return doctorScheduleMapper.selectList(Wrappers.<DoctorSchedule>query()
                        .eq("doctor_id", doctor.getId())
                        .ge("schedule_date", today)
                        .orderByAsc("schedule_date")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(DoctorScheduleVO.class))
                .toList();
    }

    private Doctor currentDoctor(int accountId) {
        return doctorMapper.selectOne(Wrappers.<Doctor>query()
                .eq("account_id", accountId)
                .last("limit 1"));
    }

    private boolean isRiskConsultation(AdminConsultationRecordVO record) {
        if (record.getTriageActionType() == null) return false;
        return switch (record.getTriageActionType()) {
            case "emergency", "offline" -> true;
            default -> false;
        };
    }

    private boolean isSameDay(Date date, LocalDate day) {
        if (date == null) return false;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(day);
    }
}
