package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.dto.ConsultationDoctorFollowUp;
import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.vo.request.DoctorConsultationAssignSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFollowUpSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationHandleSubmitVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;
import cn.gugufish.entity.vo.response.TriageRuleHitLogVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.mapper.ConsultationDoctorFollowUpMapper;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.service.ConsultationDoctorAssignmentQueryService;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import cn.gugufish.service.DoctorWorkspaceService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import cn.gugufish.utils.ConsultationAiComparisonUtils;
import cn.gugufish.utils.ConsultationAiMismatchReasonUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Resource
    ConsultationDoctorHandleMapper consultationDoctorHandleMapper;

    @Resource
    ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper;

    @Resource
    ConsultationDoctorFollowUpMapper consultationDoctorFollowUpMapper;

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

    @Resource
    ConsultationDoctorAssignmentQueryService consultationDoctorAssignmentQueryService;

    @Resource
    ConsultationDoctorHandleQueryService consultationDoctorHandleQueryService;

    @Resource
    ConsultationDoctorConclusionQueryService consultationDoctorConclusionQueryService;

    @Resource
    ConsultationDoctorFollowUpQueryService consultationDoctorFollowUpQueryService;

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

        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .eq("department_id", doctor.getDepartmentId())
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) return List.of();

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, ConsultationDoctorAssignment> assignmentMap = new HashMap<>();
        consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", consultationIds))
                .forEach(item -> assignmentMap.put(item.getConsultationId(), item));

        return records.stream()
                .map(item -> item.asViewObject(AdminConsultationRecordVO.class, vo -> {
                    ConsultationDoctorAssignment assignment = assignmentMap.get(item.getId());
                    if (assignment != null) {
                        vo.setDoctorAssignment(assignment.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO.class));
                    }
                }))
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
        var doctorAssignment = consultationDoctorAssignmentQueryService.detailByConsultationId(recordId);
        var doctorHandle = consultationDoctorHandleQueryService.detailByConsultationId(recordId);
        var doctorConclusion = consultationDoctorConclusionQueryService.detailByConsultationId(recordId);
        var doctorFollowUps = consultationDoctorFollowUpQueryService.listByConsultationId(recordId);
        var triageSession = triageSessionQueryService.detailByConsultationId(recordId);
        var triageResult = triageResultQueryService.detailByConsultationId(recordId);
        var triageFeedback = triageFeedbackQueryService.detailByConsultationId(recordId);

        return record.asViewObject(AdminConsultationRecordVO.class, vo -> {
            vo.setAnswers(answers);
            vo.setRuleHits(ruleHits);
            vo.setDoctorAssignment(doctorAssignment);
            vo.setDoctorHandle(doctorHandle);
            vo.setDoctorConclusion(doctorConclusion);
            vo.setAiComparison(ConsultationAiComparisonUtils.build(doctorConclusion, triageSession, triageResult));
            vo.setDoctorFollowUps(doctorFollowUps);
            vo.setTriageSession(triageSession);
            vo.setTriageResult(triageResult);
            vo.setTriageFeedback(triageFeedback);
        });
    }

    @Override
    @Transactional
    public String claimConsultation(int accountId, DoctorConsultationAssignSubmitVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getConsultationId());
        if (record == null) return "问诊记录不存在";
        if (!doctor.getDepartmentId().equals(record.getDepartmentId())) return "当前问诊记录不属于你所在科室";
        if ("completed".equals(record.getStatus())) return "已完成的问诊单不能再次认领";

        ConsultationDoctorHandle handle = findHandle(vo.getConsultationId());
        if (handle != null && handle.getDoctorId() != null && !handle.getDoctorId().equals(doctor.getId())) {
            return "该问诊单已由医生 " + handle.getDoctorName() + " 进入处理流程";
        }

        String departmentName = resolveDepartmentName(doctor, record);
        return ensureClaimed(vo.getConsultationId(), doctor, departmentName, new Date(), false);
    }

    @Override
    @Transactional
    public String releaseConsultation(int accountId, DoctorConsultationAssignSubmitVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getConsultationId());
        if (record == null) return "问诊记录不存在";
        if (!doctor.getDepartmentId().equals(record.getDepartmentId())) return "当前问诊记录不属于你所在科室";
        if ("completed".equals(record.getStatus())) return "已完成的问诊单不能释放";

        ConsultationDoctorAssignment assignment = findAssignment(vo.getConsultationId());
        if (assignment == null || !"claimed".equals(assignment.getStatus())) {
            return "当前问诊单尚未被认领";
        }
        if (!doctor.getId().equals(assignment.getDoctorId())) {
            return "只能释放自己认领的问诊单";
        }

        ConsultationDoctorHandle handle = findHandle(vo.getConsultationId());
        if (handle != null && ("processing".equals(handle.getStatus()) || "completed".equals(handle.getStatus()))) {
            return "已进入处理流程的问诊单不能释放";
        }

        Date now = new Date();
        assignment.setStatus("released");
        assignment.setReleaseTime(now);
        assignment.setUpdateTime(now);
        if (consultationDoctorAssignmentMapper.updateById(assignment) <= 0) {
            return "问诊单释放失败";
        }
        return null;
    }

    @Override
    @Transactional
    public String submitConsultationHandle(int accountId, DoctorConsultationHandleSubmitVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getConsultationId());
        if (record == null) return "问诊记录不存在";
        if (!doctor.getDepartmentId().equals(record.getDepartmentId())) {
            return "当前问诊记录不属于你所在科室，无法提交处理结果";
        }

        String status = trimToNull(vo.getStatus());
        String summary = trimToNull(vo.getSummary());
        String medicalAdvice = trimToNull(vo.getMedicalAdvice());
        String followUpPlan = trimToNull(vo.getFollowUpPlan());
        String internalRemark = trimToNull(vo.getInternalRemark());

        String conditionLevel = trimToNull(vo.getConditionLevel());
        String disposition = trimToNull(vo.getDisposition());
        String diagnosisDirection = trimToNull(vo.getDiagnosisDirection());
        List<String> conclusionTags = normalizeConclusionTags(vo.getConclusionTags());
        Integer needFollowUp = vo.getNeedFollowUp() == null ? 0 : vo.getNeedFollowUp();
        Integer followUpWithinDays = vo.getFollowUpWithinDays();
        Integer isConsistentWithAi = vo.getIsConsistentWithAi();
        List<String> aiMismatchReasons = ConsultationAiMismatchReasonUtils.normalizeCodes(vo.getAiMismatchReasons());
        String aiMismatchRemark = trimToNull(vo.getAiMismatchRemark());
        String patientInstruction = trimToNull(vo.getPatientInstruction());

        if (!Objects.equals(isConsistentWithAi, 0)) {
            aiMismatchReasons = List.of();
            aiMismatchRemark = null;
        }

        if (needFollowUp != 1) {
            followUpWithinDays = null;
        }

        if (summary == null) return "请填写医生判断摘要";
        if ("completed".equals(status) && medicalAdvice == null) return "完成处理时请填写处理建议";
        if ("completed".equals(status) && conditionLevel == null) return "完成处理时请填写病情等级";
        if ("completed".equals(status) && disposition == null) return "完成处理时请填写处理去向";
        if ("completed".equals(status) && isConsistentWithAi == null) return "完成处理时请填写是否与 AI 建议一致";
        if ("completed".equals(status) && Objects.equals(isConsistentWithAi, 0)
                && aiMismatchReasons.isEmpty() && aiMismatchRemark == null) {
            return "涓?AI 涓嶄竴鑷存椂璇疯嚦灏戦€夋嫨涓€涓樊寮傚師鍥犳垨濉啓琛ュ厖璇存槑";
        }
        if ("completed".equals(status) && needFollowUp == 1 && followUpWithinDays == null) {
            return "需要随访时请填写随访时效";
        }

        ConsultationDoctorAssignment assignment = findAssignment(vo.getConsultationId());
        if (assignment != null && "claimed".equals(assignment.getStatus()) && !doctor.getId().equals(assignment.getDoctorId())) {
            return "该问诊单已由医生 " + assignment.getDoctorName() + " 认领";
        }

        ConsultationDoctorHandle handle = findHandle(vo.getConsultationId());
        if (handle != null && handle.getDoctorId() != null && !handle.getDoctorId().equals(doctor.getId())) {
            return "该问诊单已由医生 " + handle.getDoctorName() + " 进入处理流程";
        }
        if (handle != null && "completed".equals(handle.getStatus()) && "processing".equals(status)) {
            return "已完成的问诊单不能回退到处理中";
        }

        Date now = new Date();
        String departmentName = resolveDepartmentName(doctor, record);
        String claimMessage = ensureClaimed(vo.getConsultationId(), doctor, departmentName, now, true);
        if (claimMessage != null) return claimMessage;

        if (handle == null) {
            handle = new ConsultationDoctorHandle(
                    null,
                    vo.getConsultationId(),
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getDepartmentId(),
                    departmentName,
                    status,
                    summary,
                    medicalAdvice,
                    followUpPlan,
                    internalRemark,
                    now,
                    "completed".equals(status) ? now : null,
                    now,
                    now
            );
            if (consultationDoctorHandleMapper.insert(handle) <= 0) {
                return "医生处理结果保存失败";
            }
        } else {
            handle.setDoctorId(doctor.getId());
            handle.setDoctorName(doctor.getName());
            handle.setDepartmentId(doctor.getDepartmentId());
            handle.setDepartmentName(departmentName);
            handle.setStatus(status);
            handle.setSummary(summary);
            handle.setMedicalAdvice(medicalAdvice);
            handle.setFollowUpPlan(followUpPlan);
            handle.setInternalRemark(internalRemark);
            if (handle.getReceiveTime() == null) handle.setReceiveTime(now);
            if ("completed".equals(status)) handle.setCompleteTime(now);
            handle.setUpdateTime(now);
            if (consultationDoctorHandleMapper.updateById(handle) <= 0) {
                return "医生处理结果更新失败";
            }
        }

        String conclusionMessage = saveDoctorConclusion(vo.getConsultationId(), doctor, departmentName, status,
                conditionLevel, disposition, diagnosisDirection, conclusionTags, needFollowUp,
                followUpWithinDays, isConsistentWithAi, aiMismatchReasons, aiMismatchRemark, patientInstruction, now);
        if (conclusionMessage != null) return conclusionMessage;

        record.setStatus(status);
        record.setUpdateTime(now);
        if (consultationRecordMapper.updateById(record) <= 0) {
            return "问诊状态更新失败";
        }
        return null;
    }

    @Override
    @Transactional
    public String submitConsultationFollowUp(int accountId, DoctorConsultationFollowUpSubmitVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getConsultationId());
        if (record == null) return "问诊记录不存在";
        if (!doctor.getDepartmentId().equals(record.getDepartmentId())) return "当前问诊记录不属于你所在科室";
        if (!"completed".equals(record.getStatus())) return "仅已完成的问诊单才可追加随访记录";

        ConsultationDoctorHandle handle = findHandle(vo.getConsultationId());
        if (handle == null || !"completed".equals(handle.getStatus())) {
            return "当前问诊单尚未完成医生处理，暂不可记录随访";
        }
        if (!doctor.getId().equals(handle.getDoctorId())) {
            return "仅处理该问诊单的医生可追加随访记录";
        }

        String summary = trimToNull(vo.getSummary());
        String advice = trimToNull(vo.getAdvice());
        String nextStep = trimToNull(vo.getNextStep());
        Integer needRevisit = vo.getNeedRevisit() != null && vo.getNeedRevisit() == 1 ? 1 : 0;
        String nextFollowUpDateText = trimToNull(vo.getNextFollowUpDate());
        if (summary == null) return "请填写随访摘要";
        if (needRevisit == 1 && nextFollowUpDateText == null) return "需要再次随访时请填写下次随访日期";

        Date nextFollowUpDate = null;
        if (nextFollowUpDateText != null) {
            try {
                java.sql.Date parsedDate = java.sql.Date.valueOf(nextFollowUpDateText);
                if (needRevisit == 1 && parsedDate.toLocalDate().isBefore(LocalDate.now())) {
                    return "下次随访日期不能早于今天";
                }
                nextFollowUpDate = parsedDate;
            } catch (IllegalArgumentException exception) {
                return "下次随访日期格式不正确";
            }
        }

        Date now = new Date();
        String departmentName = resolveDepartmentName(doctor, record);
        ConsultationDoctorFollowUp followUp = new ConsultationDoctorFollowUp(
                null,
                vo.getConsultationId(),
                doctor.getId(),
                doctor.getName(),
                doctor.getDepartmentId(),
                departmentName,
                trimToNull(vo.getFollowUpType()),
                trimToNull(vo.getPatientStatus()),
                summary,
                advice,
                nextStep,
                needRevisit,
                nextFollowUpDate,
                now,
                now
        );
        return consultationDoctorFollowUpMapper.insert(followUp) > 0 ? null : "随访记录保存失败";
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

    private String saveDoctorConclusion(int consultationId,
                                        Doctor doctor,
                                        String departmentName,
                                        String status,
                                        String conditionLevel,
                                        String disposition,
                                        String diagnosisDirection,
                                        List<String> conclusionTags,
                                        Integer needFollowUp,
                                        Integer followUpWithinDays,
                                        Integer isConsistentWithAi,
                                        List<String> aiMismatchReasons,
                                        String aiMismatchRemark,
                                        String patientInstruction,
                                        Date now) {
        ConsultationDoctorConclusion conclusion = consultationDoctorConclusionMapper.selectOne(Wrappers.<ConsultationDoctorConclusion>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));

        boolean shouldSave = conclusion != null
                || "completed".equals(status)
                || hasAnyConclusionData(conditionLevel, disposition, diagnosisDirection, conclusionTags,
                needFollowUp, followUpWithinDays, isConsistentWithAi, aiMismatchReasons, aiMismatchRemark, patientInstruction);

        if (!shouldSave) return null;

        String tagsJson = conclusionTags.isEmpty() ? null : JSON.toJSONString(conclusionTags);
        String aiMismatchReasonsJson = ConsultationAiMismatchReasonUtils.toJson(aiMismatchReasons);
        if (conclusion == null) {
            conclusion = new ConsultationDoctorConclusion(
                    null,
                    consultationId,
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getDepartmentId(),
                    departmentName,
                    conditionLevel,
                    disposition,
                    diagnosisDirection,
                    tagsJson,
                    needFollowUp,
                    followUpWithinDays,
                    isConsistentWithAi,
                    aiMismatchReasonsJson,
                    aiMismatchRemark,
                    patientInstruction,
                    now,
                    now
            );
            if (consultationDoctorConclusionMapper.insert(conclusion) <= 0) {
                return "医生结构化结论保存失败";
            }
        } else {
            conclusion.setDoctorId(doctor.getId());
            conclusion.setDoctorName(doctor.getName());
            conclusion.setDepartmentId(doctor.getDepartmentId());
            conclusion.setDepartmentName(departmentName);
            conclusion.setConditionLevel(conditionLevel);
            conclusion.setDisposition(disposition);
            conclusion.setDiagnosisDirection(diagnosisDirection);
            conclusion.setConclusionTagsJson(tagsJson);
            conclusion.setNeedFollowUp(needFollowUp);
            conclusion.setFollowUpWithinDays(followUpWithinDays);
            conclusion.setIsConsistentWithAi(isConsistentWithAi);
            conclusion.setAiMismatchReasonsJson(aiMismatchReasonsJson);
            conclusion.setAiMismatchRemark(aiMismatchRemark);
            conclusion.setPatientInstruction(patientInstruction);
            conclusion.setUpdateTime(now);
            if (consultationDoctorConclusionMapper.updateById(conclusion) <= 0) {
                return "医生结构化结论更新失败";
            }
        }
        return null;
    }

    private String ensureClaimed(int consultationId,
                                 Doctor doctor,
                                 String departmentName,
                                 Date now,
                                 boolean silentWhenOwnedBySelf) {
        ConsultationDoctorAssignment assignment = findAssignment(consultationId);
        if (assignment == null) {
            assignment = new ConsultationDoctorAssignment(
                    null,
                    consultationId,
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getDepartmentId(),
                    departmentName,
                    "claimed",
                    now,
                    null,
                    now,
                    now
            );
            if (consultationDoctorAssignmentMapper.insert(assignment) <= 0) {
                return "问诊认领失败";
            }
            return null;
        }

        if ("claimed".equals(assignment.getStatus()) && doctor.getId().equals(assignment.getDoctorId())) {
            return silentWhenOwnedBySelf ? null : "当前问诊单已由你认领";
        }
        if ("claimed".equals(assignment.getStatus()) && !doctor.getId().equals(assignment.getDoctorId())) {
            return "当前问诊单已由医生 " + assignment.getDoctorName() + " 认领";
        }

        assignment.setDoctorId(doctor.getId());
        assignment.setDoctorName(doctor.getName());
        assignment.setDepartmentId(doctor.getDepartmentId());
        assignment.setDepartmentName(departmentName);
        assignment.setStatus("claimed");
        assignment.setClaimTime(now);
        assignment.setReleaseTime(null);
        assignment.setUpdateTime(now);
        if (consultationDoctorAssignmentMapper.updateById(assignment) <= 0) {
            return "问诊认领失败";
        }
        return null;
    }

    private Doctor validDoctor(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return null;
        if (doctor.getStatus() == null || doctor.getStatus() != 1) return null;
        if (doctor.getDepartmentId() == null) return null;
        return doctor;
    }

    private ConsultationDoctorAssignment findAssignment(int consultationId) {
        return consultationDoctorAssignmentMapper.selectOne(Wrappers.<ConsultationDoctorAssignment>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
    }

    private ConsultationDoctorHandle findHandle(int consultationId) {
        return consultationDoctorHandleMapper.selectOne(Wrappers.<ConsultationDoctorHandle>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
    }

    private String resolveDepartmentName(Doctor doctor, ConsultationRecord record) {
        Department department = departmentMapper.selectById(doctor.getDepartmentId());
        return department == null ? record.getDepartmentName() : department.getName();
    }

    private boolean hasAnyConclusionData(String conditionLevel,
                                         String disposition,
                                         String diagnosisDirection,
                                         List<String> conclusionTags,
                                         Integer needFollowUp,
                                         Integer followUpWithinDays,
                                         Integer isConsistentWithAi,
                                         List<String> aiMismatchReasons,
                                         String aiMismatchRemark,
                                         String patientInstruction) {
        return conditionLevel != null
                || disposition != null
                || diagnosisDirection != null
                || (conclusionTags != null && !conclusionTags.isEmpty())
                || (needFollowUp != null && needFollowUp == 1)
                || followUpWithinDays != null
                || isConsistentWithAi != null
                || (aiMismatchReasons != null && !aiMismatchReasons.isEmpty())
                || aiMismatchRemark != null
                || patientInstruction != null;
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

    private List<String> normalizeConclusionTags(List<String> source) {
        if (source == null || source.isEmpty()) return List.of();
        return source.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .limit(8)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
