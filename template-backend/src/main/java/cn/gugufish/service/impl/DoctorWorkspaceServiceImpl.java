package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.entity.dto.ConsultationCheckSuggestion;
import cn.gugufish.entity.dto.ConsultationServiceFeedback;
import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.dto.ConsultationDoctorFollowUp;
import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationMedicationFeedback;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.ConsultationReportFeedback;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorFormAiLog;
import cn.gugufish.entity.dto.DoctorMessageAiLog;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.vo.request.ConsultationPrescriptionPreviewRequestVO;
import cn.gugufish.entity.vo.request.DoctorConsultationAiDraftGenerateVO;
import cn.gugufish.entity.vo.request.DoctorConsultationAssignSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFollowUpSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFormDraftApplyVO;
import cn.gugufish.entity.vo.request.DoctorCheckSuggestionItemSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationHandleSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationMessageDraftApplyVO;
import cn.gugufish.entity.vo.request.DoctorConsultationMessageDraftGenerateVO;
import cn.gugufish.entity.vo.request.DoctorConsultationServiceFeedbackHandleSubmitVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationCheckSuggestionVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorFollowUpVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO;
import cn.gugufish.entity.vo.response.ConsultationMedicationFeedbackVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
import cn.gugufish.entity.vo.response.ConsultationMessageSummaryVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionPreviewVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.ConsultationReportFeedbackVO;
import cn.gugufish.entity.vo.response.ConsultationServiceFeedbackVO;
import cn.gugufish.entity.vo.response.DoctorConsultationFollowUpDraftVO;
import cn.gugufish.entity.vo.response.DoctorConsultationHandleDraftVO;
import cn.gugufish.entity.vo.response.DoctorConsultationMessageDraftVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;
import cn.gugufish.entity.vo.response.MedicineCatalogVO;
import cn.gugufish.entity.vo.response.TriageRuleHitLogVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.mapper.ConsultationDoctorFollowUpMapper;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.mapper.ConsultationCheckSuggestionMapper;
import cn.gugufish.mapper.ConsultationMedicationFeedbackMapper;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.ConsultationReportFeedbackMapper;
import cn.gugufish.mapper.ConsultationServiceFeedbackMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorFormAiLogMapper;
import cn.gugufish.mapper.DoctorMessageAiLogMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.service.ConsultationMessageService;
import cn.gugufish.service.ConsultationDoctorAssignmentQueryService;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import cn.gugufish.service.ConsultationPaymentService;
import cn.gugufish.service.ConsultationPrescriptionService;
import cn.gugufish.service.ConsultationServiceFeedbackQueryService;
import cn.gugufish.service.DoctorWorkspaceService;
import cn.gugufish.service.MedicineCatalogService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import cn.gugufish.utils.ConsultationAiComparisonUtils;
import cn.gugufish.utils.ConsultationArchiveSummaryUtils;
import cn.gugufish.utils.ConsultationAiMismatchReasonUtils;
import cn.gugufish.utils.ConsultationSmartDispatchUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DoctorWorkspaceServiceImpl implements DoctorWorkspaceService {

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    DoctorMessageAiLogMapper doctorMessageAiLogMapper;

    @Resource
    DoctorFormAiLogMapper doctorFormAiLogMapper;

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    DoctorServiceTagMapper doctorServiceTagMapper;

    @Resource
    DoctorScheduleMapper doctorScheduleMapper;

    @Resource
    TriageResultMapper triageResultMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Resource
    ConsultationDoctorHandleMapper consultationDoctorHandleMapper;

    @Resource
    ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper;

    @Resource
    ConsultationServiceFeedbackMapper consultationServiceFeedbackMapper;

    @Resource
    ConsultationDoctorFollowUpMapper consultationDoctorFollowUpMapper;

    @Resource
    ConsultationCheckSuggestionMapper consultationCheckSuggestionMapper;

    @Resource
    ConsultationReportFeedbackMapper consultationReportFeedbackMapper;

    @Resource
    ConsultationMedicationFeedbackMapper consultationMedicationFeedbackMapper;

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

    @Resource
    ConsultationServiceFeedbackQueryService consultationServiceFeedbackQueryService;

    @Resource
    ConsultationMessageService consultationMessageService;

    @Resource
    MedicineCatalogService medicineCatalogService;

    @Resource
    ConsultationPrescriptionService consultationPrescriptionService;

    @Resource
    ConsultationPaymentService consultationPaymentService;

    @Resource
    AiTriageProperties aiTriageProperties;

    @Resource
    Environment environment;

    @Autowired
    ObjectProvider<DeepSeekChatModel> chatModelProvider;

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
            vo.setUnclaimedConsultationCount(0);
            vo.setMyClaimedConsultationCount(0);
            vo.setHighPriorityUnclaimedCount(0);
            vo.setUnreadConsultationCount(0);
            vo.setWaitingReplyConsultationCount(0);
            vo.setPendingFollowUpCount(0);
            vo.setDueTodayFollowUpCount(0);
            vo.setOverdueFollowUpCount(0);
            vo.setRecommendedConsultationCount(0);
            vo.setUpcomingScheduleCount(0);
            vo.setServiceTagCount(0);
            vo.setServiceFeedbackCount(0);
            vo.setResolvedServiceFeedbackCount(0);
            vo.setUnresolvedServiceFeedbackCount(0);
            vo.setLowScoreServiceFeedbackCount(0);
            vo.setAttentionServiceFeedbackCount(0);
            vo.setAverageServiceScore(null);
            vo.setResolvedServiceFeedbackRate(null);
            vo.setServiceTags(List.of());
            vo.setRecentServiceFeedbacks(List.of());
            vo.setRecentConsultations(List.of());
            vo.setUnclaimedConsultations(List.of());
            vo.setRecommendedConsultations(List.of());
            vo.setUnreadConsultations(List.of());
            vo.setWaitingReplyConsultations(List.of());
            vo.setPendingFollowUpConsultations(List.of());
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
        List<ConsultationServiceFeedbackVO> serviceFeedbacks = consultationServiceFeedbackQueryService.listByDoctorId(doctor.getId());

        LocalDate today = LocalDate.now();
        int todayCount = (int) consultations.stream()
                .filter(item -> isSameDay(item.getCreateTime(), today))
                .count();
        int riskCount = (int) consultations.stream()
                .filter(this::isRiskConsultation)
                .count();
        List<AdminConsultationRecordVO> unclaimedConsultations = buildUnclaimedConsultations(consultations);
        int myClaimedConsultationCount = (int) consultations.stream()
                .filter(item -> isClaimedByDoctor(item, doctor.getId()))
                .count();
        int highPriorityUnclaimedCount = (int) unclaimedConsultations.stream()
                .filter(this::isRiskConsultation)
                .count();
        List<AdminConsultationRecordVO> recommendedConsultations = buildRecommendedConsultations(consultations, doctor.getId());
        List<AdminConsultationRecordVO> unreadConsultations = buildUnreadConsultations(consultations, doctor.getId());
        List<AdminConsultationRecordVO> waitingReplyConsultations = buildWaitingReplyConsultations(consultations, doctor.getId());
        List<AdminConsultationRecordVO> pendingFollowUpConsultations = buildPendingFollowUpConsultations(consultations, doctor.getId());
        int dueTodayFollowUpCount = (int) pendingFollowUpConsultations.stream()
                .filter(this::isDueTodayFollowUp)
                .count();
        int overdueFollowUpCount = (int) pendingFollowUpConsultations.stream()
                .filter(this::isOverdueFollowUp)
                .count();
        int resolvedServiceFeedbackCount = (int) serviceFeedbacks.stream()
                .filter(item -> Objects.equals(item.getIsResolved(), 1))
                .count();
        int unresolvedServiceFeedbackCount = serviceFeedbacks.size() - resolvedServiceFeedbackCount;
        int lowScoreServiceFeedbackCount = (int) serviceFeedbacks.stream()
                .filter(item -> item.getServiceScore() != null && item.getServiceScore() <= 2)
                .count();
        int attentionServiceFeedbackCount = (int) serviceFeedbacks.stream()
                .filter(this::isAttentionServiceFeedback)
                .count();
        Double averageServiceScore = averageServiceScore(serviceFeedbacks);
        Double resolvedServiceFeedbackRate = serviceFeedbacks.isEmpty()
                ? null
                : roundToOneDecimal(resolvedServiceFeedbackCount * 100D / serviceFeedbacks.size());

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
        vo.setUnclaimedConsultationCount(unclaimedConsultations.size());
        vo.setMyClaimedConsultationCount(myClaimedConsultationCount);
        vo.setHighPriorityUnclaimedCount(highPriorityUnclaimedCount);
        vo.setUnreadConsultationCount(unreadConsultations.size());
        vo.setWaitingReplyConsultationCount(waitingReplyConsultations.size());
        vo.setPendingFollowUpCount(pendingFollowUpConsultations.size());
        vo.setDueTodayFollowUpCount(dueTodayFollowUpCount);
        vo.setOverdueFollowUpCount(overdueFollowUpCount);
        vo.setRecommendedConsultationCount(recommendedConsultations.size());
        vo.setUpcomingScheduleCount(schedules.size());
        vo.setServiceTagCount(serviceTags.size());
        vo.setServiceFeedbackCount(serviceFeedbacks.size());
        vo.setResolvedServiceFeedbackCount(resolvedServiceFeedbackCount);
        vo.setUnresolvedServiceFeedbackCount(unresolvedServiceFeedbackCount);
        vo.setLowScoreServiceFeedbackCount(lowScoreServiceFeedbackCount);
        vo.setAttentionServiceFeedbackCount(attentionServiceFeedbackCount);
        vo.setAverageServiceScore(averageServiceScore);
        vo.setResolvedServiceFeedbackRate(resolvedServiceFeedbackRate);
        vo.setServiceTags(serviceTags);
        vo.setRecentServiceFeedbacks(serviceFeedbacks.stream().limit(5).toList());
        vo.setRecentConsultations(consultations.stream().limit(6).toList());
        vo.setUnclaimedConsultations(unclaimedConsultations.stream().limit(5).toList());
        vo.setRecommendedConsultations(recommendedConsultations.stream().limit(5).toList());
        vo.setUnreadConsultations(unreadConsultations.stream().limit(5).toList());
        vo.setWaitingReplyConsultations(waitingReplyConsultations.stream().limit(5).toList());
        vo.setPendingFollowUpConsultations(pendingFollowUpConsultations.stream().limit(5).toList());
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
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> assignmentMap.putIfAbsent(item.getConsultationId(), item));
        Map<Integer, TriageResult> triageResultMap = new HashMap<>();
        triageResultMapper.selectList(Wrappers.<TriageResult>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("is_final")
                        .orderByDesc("id"))
                .forEach(item -> triageResultMap.putIfAbsent(item.getConsultationId(), item));
        Map<Integer, ConsultationDoctorHandle> handleMap = new HashMap<>();
        consultationDoctorHandleMapper.selectList(Wrappers.<ConsultationDoctorHandle>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> handleMap.putIfAbsent(item.getConsultationId(), item));
        Map<Integer, ConsultationDoctorConclusion> conclusionMap = new HashMap<>();
        consultationDoctorConclusionMapper.selectList(Wrappers.<ConsultationDoctorConclusion>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> conclusionMap.putIfAbsent(item.getConsultationId(), item));
        Map<Integer, ConsultationDoctorFollowUp> latestFollowUpMap = new HashMap<>();
        consultationDoctorFollowUpMapper.selectList(Wrappers.<ConsultationDoctorFollowUp>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .forEach(item -> latestFollowUpMap.putIfAbsent(item.getConsultationId(), item));
        Map<Integer, ConsultationMessageSummaryVO> messageSummaryMap = consultationMessageService.summarizeDoctorMessages(consultationIds);
        Map<Integer, cn.gugufish.entity.vo.response.ConsultationPaymentVO> paymentMap = consultationPaymentService.mapByConsultationIds(consultationIds);
        Map<Integer, ConsultationServiceFeedbackVO> serviceFeedbackMap = consultationServiceFeedbackQueryService.mapByConsultationIds(consultationIds);

        return records.stream()
                .map(item -> item.asViewObject(AdminConsultationRecordVO.class, vo -> {
                    ConsultationDoctorAssignment assignment = assignmentMap.get(item.getId());
                    TriageResult triageResult = triageResultMap.get(item.getId());
                    ConsultationDoctorHandle handle = handleMap.get(item.getId());
                    ConsultationDoctorConclusion conclusion = conclusionMap.get(item.getId());
                    ConsultationDoctorFollowUp latestFollowUp = latestFollowUpMap.get(item.getId());
                    if (assignment != null) {
                        vo.setDoctorAssignment(assignment.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO.class));
                    }
                    if (handle != null) {
                        vo.setDoctorHandle(handle.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO.class));
                    }
                    if (conclusion != null) {
                        vo.setDoctorConclusion(conclusion.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO.class));
                    }
                    vo.setDoctorFollowUps(latestFollowUp == null
                            ? List.of()
                            : List.of(latestFollowUp.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorFollowUpVO.class)));
                    vo.setMessageSummary(messageSummaryMap.get(item.getId()));
                    vo.setPayment(paymentMap.get(item.getId()));
                    vo.setServiceFeedback(serviceFeedbackMap.get(item.getId()));
                    vo.setSmartDispatch(ConsultationSmartDispatchUtils.build(
                            assignment == null ? null : assignment.getDoctorId(),
                            assignment == null ? null : assignment.getDoctorName(),
                            assignment == null ? null : assignment.getStatus(),
                            triageResult == null ? null : triageResult.getDoctorId(),
                            triageResult == null ? null : triageResult.getDoctorName(),
                            triageResult == null ? null : triageResult.getDoctorCandidatesJson(),
                            triageResult == null ? null : triageResult.getReasonText()
                    ));
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
        var payment = consultationPaymentService.detailByConsultationId(recordId);
        var checkSuggestions = listCheckSuggestions(recordId);
        var reportFeedbacks = listReportFeedbacks(recordId);
        var medicationFeedbacks = listMedicationFeedbacks(recordId);
        var prescriptions = consultationPrescriptionService.listByConsultationId(recordId);
        var doctorFollowUps = consultationDoctorFollowUpQueryService.listByConsultationId(recordId);
        var triageSession = triageSessionQueryService.detailByConsultationId(recordId);
        var triageResult = triageResultQueryService.detailByConsultationId(recordId);
        var triageFeedback = triageFeedbackQueryService.detailByConsultationId(recordId);
        var serviceFeedback = consultationServiceFeedbackQueryService.detailByConsultationId(recordId);
        ConsultationMessageSummaryVO messageSummary = consultationMessageService.summarizeDoctorMessages(List.of(recordId)).get(recordId);

        return record.asViewObject(AdminConsultationRecordVO.class, vo -> {
            vo.setAnswers(answers);
            vo.setRuleHits(ruleHits);
            vo.setDoctorAssignment(doctorAssignment);
            vo.setDoctorHandle(doctorHandle);
            vo.setDoctorConclusion(doctorConclusion);
            vo.setPayment(payment);
            vo.setCheckSuggestions(checkSuggestions);
            vo.setReportFeedbacks(reportFeedbacks);
            vo.setMedicationFeedbacks(medicationFeedbacks);
            vo.setPrescriptions(prescriptions);
            vo.setSmartDispatch(ConsultationSmartDispatchUtils.build(
                    doctorAssignment == null ? null : doctorAssignment.getDoctorId(),
                    doctorAssignment == null ? null : doctorAssignment.getDoctorName(),
                    doctorAssignment == null ? null : doctorAssignment.getStatus(),
                    triageResult == null ? null : triageResult.getDoctorId(),
                    triageResult == null ? null : triageResult.getDoctorName(),
                    triageResult == null ? null : triageResult.getDoctorCandidatesJson(),
                    triageResult == null ? null : triageResult.getReasonText()
            ));
            vo.setMessageSummary(messageSummary);
            vo.setArchiveSummary(ConsultationArchiveSummaryUtils.buildDoctorSummary(
                    record,
                    messageSummary,
                    doctorAssignment,
                    doctorHandle,
                    doctorConclusion,
                    doctorFollowUps,
                    triageResult,
                    serviceFeedback
            ));
            vo.setAiComparison(ConsultationAiComparisonUtils.build(doctorConclusion, triageSession, triageResult));
            vo.setDoctorFollowUps(doctorFollowUps);
            vo.setTriageSession(triageSession);
            vo.setTriageResult(triageResult);
            vo.setTriageFeedback(triageFeedback);
            vo.setServiceFeedback(serviceFeedback);
        });
    }

    @Override
    public List<MedicineCatalogVO> medicineOptions(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return List.of();
        return medicineCatalogService.listMedicines(true);
    }

    @Override
    public ConsultationPrescriptionPreviewVO previewConsultationPrescription(int accountId, ConsultationPrescriptionPreviewRequestVO vo) {
        if (vo == null || vo.getConsultationId() == null) return null;
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null || doctor.getDepartmentId() == null) return null;

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getConsultationId());
        if (record == null || !doctor.getDepartmentId().equals(record.getDepartmentId())) return null;

        return consultationPrescriptionService.preview(vo.getPrescriptions());
    }

    @Override
    public DoctorConsultationHandleDraftVO generateConsultationHandleDraft(int accountId, DoctorConsultationAiDraftGenerateVO vo) {
        if (vo == null || vo.getRecordId() == null) return null;
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return null;

        AdminConsultationRecordVO record = consultationDetail(accountId, vo.getRecordId());
        if (record == null) return null;

        List<ConsultationMessageVO> messages = consultationMessageService.listDoctorMessages(accountId, vo.getRecordId());
        List<ConsultationMessageVO> consultationMessages = messages == null ? List.of() : messages;
        String targetField = normalizeHandleDraftRegenerateField(vo.getRegenerateField());
        String rewriteRequirement = normalizeRewriteRequirement(vo.getRewriteRequirement());
        String currentDraftJson = normalizeCurrentDraftJson(vo.getCurrentDraftJson());

        DoctorConsultationHandleDraftVO draft = generateAiHandleDraft(doctor, record, consultationMessages, targetField,
                rewriteRequirement, currentDraftJson);
        if (draft == null) {
            draft = buildFallbackHandleDraft(record, consultationMessages, targetField);
        }
        draft = normalizeHandleDraft(draft, true);
        if (draft == null) return null;
        draft.setPromptVersion(resolveDoctorHandlePromptVersion(targetField));
        draft.setGenerationSummary(resolveTargetedGenerationSummary(draft.getGenerationSummary(), renderHandleDraftFieldLabel(targetField)));
        DoctorFormAiLog logRecord = saveDoctorFormAiLog(doctor, record, "handle", targetField, draft.getSource(), draft.getPromptVersion(),
                draft.getFallback(), draft.getRiskFlags(), draft.getGenerationSummary(),
                hasDraftRewriteContext(rewriteRequirement, currentDraftJson), rewriteRequirement,
                buildHandleDraftPreview(draft), JSON.toJSONString(draft));
        if (logRecord != null) {
            draft.setLogId(logRecord.getId());
        }
        return draft;
    }

    @Override
    public DoctorConsultationFollowUpDraftVO generateConsultationFollowUpDraft(int accountId, DoctorConsultationAiDraftGenerateVO vo) {
        if (vo == null || vo.getRecordId() == null) return null;
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return null;

        AdminConsultationRecordVO record = consultationDetail(accountId, vo.getRecordId());
        if (record == null) return null;

        List<ConsultationMessageVO> messages = consultationMessageService.listDoctorMessages(accountId, vo.getRecordId());
        List<ConsultationMessageVO> consultationMessages = messages == null ? List.of() : messages;
        String targetField = normalizeFollowUpDraftRegenerateField(vo.getRegenerateField());
        String rewriteRequirement = normalizeRewriteRequirement(vo.getRewriteRequirement());
        String currentDraftJson = normalizeCurrentDraftJson(vo.getCurrentDraftJson());

        DoctorConsultationFollowUpDraftVO draft = generateAiFollowUpDraft(doctor, record, consultationMessages, targetField,
                rewriteRequirement, currentDraftJson);
        if (draft == null) {
            draft = buildFallbackFollowUpDraft(record, consultationMessages, targetField);
        }
        draft = normalizeFollowUpDraft(draft, true);
        if (draft == null) return null;
        draft.setPromptVersion(resolveDoctorFollowUpPromptVersion(targetField));
        draft.setGenerationSummary(resolveTargetedGenerationSummary(draft.getGenerationSummary(), renderFollowUpDraftFieldLabel(targetField)));
        DoctorFormAiLog logRecord = saveDoctorFormAiLog(doctor, record, "follow_up", targetField, draft.getSource(), draft.getPromptVersion(),
                draft.getFallback(), draft.getRiskFlags(), draft.getGenerationSummary(),
                hasDraftRewriteContext(rewriteRequirement, currentDraftJson), rewriteRequirement,
                buildFollowUpDraftPreview(draft), JSON.toJSONString(draft));
        if (logRecord != null) {
            draft.setLogId(logRecord.getId());
        }
        return draft;
    }

    @Override
    @Transactional
    public String trackConsultationFormDraftApply(int accountId, DoctorConsultationFormDraftApplyVO vo) {
        if (vo == null || vo.getLogId() == null || vo.getRecordId() == null) return "AI 草稿记录不存在";
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        DoctorFormAiLog logRecord = doctorFormAiLogMapper.selectById(vo.getLogId());
        if (logRecord == null || !Objects.equals(logRecord.getDoctorId(), doctor.getId())) {
            return "AI 草稿记录不存在或暂无操作权限";
        }
        if (!Objects.equals(logRecord.getConsultationId(), vo.getRecordId())) {
            return "AI 草稿记录与当前问诊不匹配";
        }

        Date now = new Date();
        logRecord.setApplyCount(defaultInt(logRecord.getApplyCount()) + 1);
        logRecord.setLastApplyMode(normalizeDraftApplyMode(vo.getApplyMode()));
        logRecord.setLastApplyTarget(normalizeFormDraftApplyTarget(vo.getApplyTarget()));
        logRecord.setTemplateSceneType(normalizeFormTemplateSceneType(vo.getTemplateSceneType()));
        logRecord.setTemplateId(vo.getTemplateId());
        logRecord.setTemplateTitle(abbreviate(trimToNull(vo.getTemplateTitle()), 100));
        logRecord.setTemplateUsed(hasTemplateUsage(logRecord.getTemplateSceneType(), logRecord.getTemplateId(), logRecord.getTemplateTitle()) ? 1 : 0);
        if (!Objects.equals(logRecord.getTemplateUsed(), 1)) {
            logRecord.setTemplateSceneType(null);
            logRecord.setTemplateId(null);
            logRecord.setTemplateTitle(null);
        }
        logRecord.setLastApplyTime(now);
        logRecord.setUpdateTime(now);
        return doctorFormAiLogMapper.updateById(logRecord) > 0 ? null : "AI 草稿带入记录失败";
    }

    @Override
    public DoctorConsultationMessageDraftVO generateConsultationMessageDraft(int accountId, DoctorConsultationMessageDraftGenerateVO vo) {
        if (vo == null || vo.getRecordId() == null) return null;
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return null;

        AdminConsultationRecordVO record = consultationDetail(accountId, vo.getRecordId());
        if (record == null) return null;

        List<ConsultationMessageVO> messages = consultationMessageService.listDoctorMessages(accountId, vo.getRecordId());
        List<ConsultationMessageVO> consultationMessages = messages == null ? List.of() : messages;
        String sceneType = normalizeMessageDraftSceneType(vo.getSceneType(), record, consultationMessages);

        DoctorConsultationMessageDraftVO draft = generateAiMessageDraft(doctor, record, consultationMessages, sceneType);
        if (draft == null) {
            draft = buildFallbackMessageDraft(doctor, record, consultationMessages, sceneType);
        }
        draft = normalizeMessageDraft(draft, true);
        if (draft == null) return null;
        DoctorMessageAiLog logRecord = saveDoctorMessageAiLog(doctor, record, draft);
        if (logRecord != null) {
            draft.setLogId(logRecord.getId());
        }
        return draft;
    }

    @Override
    @Transactional
    public String trackConsultationMessageDraftApply(int accountId, DoctorConsultationMessageDraftApplyVO vo) {
        if (vo == null || vo.getLogId() == null || vo.getRecordId() == null) return "AI 草稿记录不存在";
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        DoctorMessageAiLog logRecord = doctorMessageAiLogMapper.selectById(vo.getLogId());
        if (logRecord == null || !Objects.equals(logRecord.getDoctorId(), doctor.getId())) {
            return "AI 草稿记录不存在或暂无操作权限";
        }
        if (!Objects.equals(logRecord.getConsultationId(), vo.getRecordId())) {
            return "AI 草稿记录与当前问诊不匹配";
        }

        Date now = new Date();
        logRecord.setApplyCount(defaultInt(logRecord.getApplyCount()) + 1);
        logRecord.setLastApplyMode(normalizeDraftApplyMode(vo.getApplyMode()));
        logRecord.setTemplateSceneType(normalizeTemplateSceneType(vo.getTemplateSceneType()));
        logRecord.setTemplateId(vo.getTemplateId());
        logRecord.setTemplateTitle(abbreviate(trimToNull(vo.getTemplateTitle()), 100));
        logRecord.setTemplateUsed(hasTemplateUsage(logRecord.getTemplateSceneType(), logRecord.getTemplateId(), logRecord.getTemplateTitle()) ? 1 : 0);
        if (!Objects.equals(logRecord.getTemplateUsed(), 1)) {
            logRecord.setTemplateSceneType(null);
            logRecord.setTemplateId(null);
            logRecord.setTemplateTitle(null);
        }
        logRecord.setLastApplyTime(now);
        logRecord.setUpdateTime(now);
        return doctorMessageAiLogMapper.updateById(logRecord) > 0 ? null : "AI 草稿带入记录失败";
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
            return "与 AI 不一致时请至少选择一个差异原因或填写补充说明";
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

        String checkSuggestionMessage = replaceConsultationCheckSuggestions(
                vo.getConsultationId(),
                doctor,
                departmentName,
                vo.getCheckSuggestions(),
                now
        );
        if (checkSuggestionMessage != null) return checkSuggestionMessage;

        String prescriptionMessage = consultationPrescriptionService.replaceConsultationPrescriptions(
                vo.getConsultationId(),
                doctor,
                vo.getPrescriptions(),
                now
        );
        if (prescriptionMessage != null) return prescriptionMessage;

        String conclusionMessage = saveDoctorConclusion(vo.getConsultationId(), doctor, departmentName, status,
                conditionLevel, disposition, diagnosisDirection, conclusionTags, needFollowUp,
                followUpWithinDays, isConsistentWithAi, aiMismatchReasons, aiMismatchRemark, patientInstruction, now);
        if (conclusionMessage != null) return conclusionMessage;

        record.setStatus(status);
        record.setUpdateTime(now);
        if (consultationRecordMapper.updateById(record) <= 0) {
            return "问诊状态更新失败";
        }
        markDoctorFormAiLogSaved(vo.getAiLogId(), doctor, record, "handle",
                buildSavedHandlePreview(vo), buildSavedHandlePayload(vo), now);
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
        if (consultationDoctorFollowUpMapper.insert(followUp) <= 0) return "随访记录保存失败";
        markDoctorFormAiLogSaved(vo.getAiLogId(), doctor, record, "follow_up",
                buildSavedFollowUpPreview(followUp), buildSavedFollowUpPayload(followUp), now);
        return null;
    }

    @Override
    @Transactional
    public String submitConsultationServiceFeedbackHandle(int accountId, DoctorConsultationServiceFeedbackHandleSubmitVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getConsultationId());
        if (record == null) return "问诊记录不存在";
        if (!doctor.getDepartmentId().equals(record.getDepartmentId())) return "当前问诊记录不属于你所在科室";

        ConsultationServiceFeedback feedback = findServiceFeedback(vo.getConsultationId());
        if (feedback == null || feedback.getStatus() == null || feedback.getStatus() != 1) return "当前问诊暂无服务评价";
        if (!doctor.getId().equals(feedback.getDoctorId())) return "仅处理该问诊单的医生可登记评价处理结果";

        String handleRemark = trimToNull(vo.getHandleRemark());
        Integer doctorHandleStatus = Objects.equals(vo.getDoctorHandleStatus(), 1) ? 1 : 0;
        if (handleRemark == null) return "请填写评价处理备注";

        Date now = new Date();
        feedback.setDoctorHandleStatus(doctorHandleStatus);
        feedback.setDoctorHandleRemark(handleRemark);
        feedback.setDoctorHandleAccountId(accountId);
        feedback.setDoctorHandleDoctorId(doctor.getId());
        feedback.setDoctorHandleDoctorName(doctor.getName());
        feedback.setDoctorHandleTime(now);
        return consultationServiceFeedbackMapper.updateById(feedback) > 0 ? null : "服务评价处理结果保存失败";
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

    private DoctorConsultationHandleDraftVO generateAiHandleDraft(Doctor doctor,
                                                                  AdminConsultationRecordVO record,
                                                                  List<ConsultationMessageVO> messages,
                                                                  String targetField,
                                                                  String rewriteRequirement,
                                                                  String currentDraftJson) {
        if (doctor == null || record == null || !isDoctorAiAvailable()) return null;

        try {
            DeepSeekChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) return null;

            DoctorConsultationHandleDraftVO draft = ChatClient.create(chatModel)
                    .prompt()
                    .system(buildDoctorHandleSystemPrompt(targetField))
                    .user(buildDoctorHandleUserPrompt(doctor, record, messages, targetField, rewriteRequirement, currentDraftJson))
                    .call()
                    .entity(DoctorConsultationHandleDraftVO.class);
            draft = normalizeHandleDraft(draft, false);
            if (draft == null) return null;
            draft.setSource("deepseek");
            draft.setFallback(0);
            draft.setPromptVersion(resolveDoctorHandlePromptVersion(targetField));
            return draft;
        } catch (Exception exception) {
            log.warn("Doctor AI handle draft generation skipped for consultation {} because DeepSeek call failed: {}",
                    record.getId(), exception.getMessage());
            return null;
        }
    }

    private String buildDoctorHandleSystemPrompt(String targetField) {
        String targetRule = buildHandleTargetFieldSystemRule(targetField);
        return """
                你是互联网医疗平台中的医生处理助手。
                你正在帮助真实医生起草“医生处理表单 + 结构化结论”草稿，供医生人工修改后保存。
                你必须遵守以下规则：
                1. 只输出 JSON，不要输出 Markdown、代码块或解释过程。
                2. 所有内容仅作为医生草稿，不得输出确定性诊断，不要编造检查结果。
                3. doctorSummary 字段写医生判断摘要，120 字以内。
                4. medicalAdvice 字段写处理建议，300 字以内。
                5. followUpPlan 字段写后续复诊/随访安排，没有就返回空字符串。
                6. conditionLevel 只能返回 low、medium、high、critical 之一。
                7. disposition 只能返回 observe、online_followup、offline_visit、emergency 之一。
                8. diagnosisDirection 字段写倾向性判断方向，避免绝对化表述。
                9. conclusionTags 最多返回 4 条，尽量从这些标签中选择：适合居家观察、建议线下检查、建议药物评估、需要复诊随访、过敏风险、发热监测、皮肤护理、慢病管理。
                10. needFollowUp 只能返回 0 或 1；若为 1，followUpWithinDays 返回 1 到 30 的整数，否则返回 null。
                11. patientInstruction 字段写给患者的安全提醒或执行要点，180 字以内。
                12. generationSummary 字段用一句话说明起草依据，便于医生快速判断是否采用。
                13. riskFlags 最多返回 4 条，只保留最值得医生关注的风险信号。
                14. 如果存在高风险，disposition 必须优先使用 offline_visit 或 emergency。
                15. 字段缺失时请返回空字符串、空数组或 null，不要输出多余字段。
                16. 如果用户补充了当前草稿快照，请优先沿用其中已经确认的结构、语气和判断，除非上下文强烈提示必须调整。
                %s""".formatted(targetRule);
    }

    private String buildDoctorHandleUserPrompt(Doctor doctor,
                                               AdminConsultationRecordVO record,
                                               List<ConsultationMessageVO> messages,
                                               String targetField,
                                               String rewriteRequirement,
                                               String currentDraftJson) {
        JSONObject aiPayload = latestAiStructuredPayload(record);
        String answerSummary = buildAnswerSummary(record);
        String conversationHistory = buildConversationHistory(messages);
        String followUpHistory = buildFollowUpHistorySummary(record);
        String aiReply = firstText(
                trimToNull(aiPayload == null ? null : aiPayload.getString("reply")),
                latestAiReplyText(record)
        );
        String nextQuestions = joinList(extractNextQuestions(aiPayload), "；");
        String riskFlags = joinList(extractRiskFlags(record), "、");
        String rewriteContextBlock = buildHandleRewriteContextBlock(currentDraftJson, rewriteRequirement);

        String prompt = """
                请结合以下问诊资料，生成一份医生可编辑的处理表单草稿。

                医生信息：
                - 医生：%s
                - 科室：%s
                - 职称：%s

                问诊信息：
                - 问诊ID：%s
                - 问诊单号：%s
                - 患者：%s
                - 问诊分类：%s
                - 当前状态：%s
                - AI 分诊等级：%s
                - AI 建议动作：%s

                主诉：
                %s

                健康摘要：
                %s

                问诊答案摘要：
                %s

                AI/规则导诊参考：
                - 导诊建议：%s
                - AI 回复参考：%s
                - 风险提示：%s
                - AI 建议补充：%s

                当前已保存医生处理：
                %s

                当前已保存结构化结论：
                %s

                历史随访摘要：
                %s

                最近沟通记录：
                %s

                生成要求：
                - 优先延续已有医生处理风格和已经沉淀的判断，再补齐缺失字段。
                - 如果当前资料不足，不要编造结论，可在 medicalAdvice 中体现继续补充观察点。
                - 如存在高风险，doctorSummary、medicalAdvice 和 patientInstruction 都要体现及时线下或急诊提醒。
                - 不要给出具体药品剂量，也不要替代医生下最终诊断。
                %s
                """.formatted(
                safeText(doctor.getName(), "-"),
                safeText(record.getDepartmentName(), "-"),
                safeText(doctor.getTitle(), "-"),
                safeNumber(record.getId()),
                safeText(record.getConsultationNo(), "-"),
                safeText(record.getPatientName(), "-"),
                safeText(record.getCategoryName(), "-"),
                safeText(record.getStatus(), "-"),
                safeText(record.getTriageLevelName(), "待评估"),
                safeText(renderVisitType(record.getTriageActionType()), "继续观察"),
                safeText(record.getChiefComplaint(), "暂无主诉"),
                safeText(record.getHealthSummary(), "暂无健康摘要"),
                safeText(answerSummary, "暂无问诊答案"),
                safeText(firstText(trimToNull(record.getTriageSuggestion()), trimToNull(record.getTriageRuleSummary())), "暂无导诊建议"),
                safeText(aiReply, "暂无 AI 导诊回复"),
                safeText(riskFlags, "暂无明确高风险提示"),
                safeText(nextQuestions, "暂无额外补充问题"),
                safeText(buildDoctorHandleSummary(record), "当前还没有医生处理记录"),
                safeText(buildDoctorConclusionSummary(record), "当前还没有结构化结论"),
                safeText(followUpHistory, "暂无历史随访记录"),
                safeText(conversationHistory, "暂无沟通记录"),
                safeText(buildHandleTargetFieldUserHint(targetField), "无额外目标字段要求。")
        );
        return StringUtils.hasText(rewriteContextBlock) ? prompt + "\n\n" + rewriteContextBlock : prompt;
    }

    private DoctorConsultationHandleDraftVO buildFallbackHandleDraft(AdminConsultationRecordVO record,
                                                                     List<ConsultationMessageVO> messages,
                                                                     String targetField) {
        DoctorConsultationHandleDraftVO draft = new DoctorConsultationHandleDraftVO();
        List<String> riskFlags = extractRiskFlags(record);
        String conditionLevel = resolveConditionLevelCode(record);
        String disposition = resolveDispositionCode(record);
        Integer needFollowUp = resolveNeedFollowUp(record, disposition);
        Integer followUpWithinDays = needFollowUp == 1 ? resolveFollowUpWithinDays(record, 3) : null;
        boolean highRisk = isHighRiskDisposition(disposition) || isRiskConsultation(record) || !riskFlags.isEmpty();

        List<String> summarySegments = new ArrayList<>();
        addSegment(summarySegments, "既往摘要", record.getDoctorHandle() == null ? null : record.getDoctorHandle().getSummary());
        addSegment(summarySegments, "主诉", record.getChiefComplaint());
        addSegment(summarySegments, "导诊建议", firstText(trimToNull(record.getTriageSuggestion()), record.getTriageResult() == null ? null : record.getTriageResult().getReasonText()));
        if (!riskFlags.isEmpty()) {
            summarySegments.add("需重点关注" + joinList(riskFlags.stream().limit(3).toList(), "、"));
        }

        List<String> adviceSegments = new ArrayList<>();
        addSegment(adviceSegments, "既往建议", record.getDoctorHandle() == null ? null : record.getDoctorHandle().getMedicalAdvice());
        if (highRisk) {
            adviceSegments.add("结合当前资料，建议优先尽快线下就医进一步评估；如症状明显加重，请直接急诊处理。");
        } else {
            addSegment(adviceSegments, "导诊回复参考", firstText(latestAiReplyText(record), trimToNull(record.getTriageSuggestion())));
            adviceSegments.add("建议继续结合症状变化、持续时间及已采取处理情况进行动态评估。");
        }
        if (!riskFlags.isEmpty()) {
            adviceSegments.add("如出现" + joinList(riskFlags.stream().limit(3).toList(), "、") + "等情况，请及时线下就医。");
        }

        List<String> followUpPlanSegments = new ArrayList<>();
        addSegment(followUpPlanSegments, "既往随访计划", record.getDoctorHandle() == null ? null : record.getDoctorHandle().getFollowUpPlan());
        if (needFollowUp == 1) {
            followUpPlanSegments.add("建议 " + followUpWithinDays + " 天内继续线上随访或复诊，必要时提前补充检查结果。");
        } else if ("offline_visit".equals(disposition)) {
            followUpPlanSegments.add("建议尽快安排线下复诊，并在检查结果明确后再决定是否追加线上随访。");
        } else if ("emergency".equals(disposition)) {
            followUpPlanSegments.add("当前更建议立即急诊，不建议继续等待线上观察。");
        }

        String diagnosisBase = firstText(trimToNull(record.getChiefComplaint()), trimToNull(record.getCategoryName()));
        draft.setDoctorSummary(abbreviate(String.join("；", summarySegments), 500));
        draft.setMedicalAdvice(abbreviate(String.join("；", adviceSegments), 1200));
        draft.setFollowUpPlan(abbreviate(String.join("；", followUpPlanSegments), 500));
        draft.setConditionLevel(conditionLevel);
        draft.setDisposition(disposition);
        draft.setDiagnosisDirection(diagnosisBase == null ? null : abbreviate("围绕" + diagnosisBase + "继续评估", 100));
        draft.setConclusionTags(buildConclusionTagSuggestions(disposition, needFollowUp, riskFlags));
        draft.setNeedFollowUp(needFollowUp);
        draft.setFollowUpWithinDays(followUpWithinDays);
        draft.setPatientInstruction(buildPatientInstructionFromRecord(record, riskFlags, highRisk));
        draft.setGenerationSummary(resolveTargetedGenerationSummary(highRisk
                ? "已结合导诊风险提示生成处理草稿，并优先补充线下就医提醒。"
                : "已结合导诊建议、当前沟通记录和既往处理内容生成处理草稿。", renderHandleDraftFieldLabel(targetField)));
        draft.setRiskFlags(riskFlags);
        draft.setPromptVersion(resolveDoctorHandlePromptVersion(targetField));
        draft.setSource("fallback");
        draft.setFallback(1);
        return draft;
    }

    private DoctorConsultationHandleDraftVO normalizeHandleDraft(DoctorConsultationHandleDraftVO draft, boolean populateMeta) {
        if (draft == null) return null;
        draft.setDoctorSummary(abbreviate(trimToNull(draft.getDoctorSummary()), 500));
        draft.setMedicalAdvice(abbreviate(trimToNull(draft.getMedicalAdvice()), 2000));
        draft.setFollowUpPlan(abbreviate(trimToNull(draft.getFollowUpPlan()), 500));
        draft.setConditionLevel(normalizeConditionLevel(draft.getConditionLevel()));
        draft.setDisposition(normalizeDisposition(draft.getDisposition()));
        draft.setDiagnosisDirection(abbreviate(trimToNull(draft.getDiagnosisDirection()), 100));
        draft.setConclusionTags(normalizeConclusionTags(draft.getConclusionTags()));
        draft.setNeedFollowUp(normalizeBinaryFlag(draft.getNeedFollowUp()));
        if (draft.getNeedFollowUp() == 1) {
            draft.setFollowUpWithinDays(normalizePositiveInt(draft.getFollowUpWithinDays(), 1, 365));
            if (draft.getFollowUpWithinDays() == null) {
                draft.setFollowUpWithinDays(3);
            }
        } else {
            draft.setFollowUpWithinDays(null);
        }
        draft.setPatientInstruction(abbreviate(trimToNull(draft.getPatientInstruction()), 500));
        draft.setGenerationSummary(abbreviate(trimToNull(draft.getGenerationSummary()), 160));
        draft.setRiskFlags(normalizeTextList(draft.getRiskFlags(), 4, 30));
        boolean hasContent = StringUtils.hasText(draft.getDoctorSummary())
                || StringUtils.hasText(draft.getMedicalAdvice())
                || StringUtils.hasText(draft.getFollowUpPlan())
                || StringUtils.hasText(draft.getConditionLevel())
                || StringUtils.hasText(draft.getDisposition())
                || StringUtils.hasText(draft.getDiagnosisDirection())
                || !draft.getConclusionTags().isEmpty()
                || StringUtils.hasText(draft.getPatientInstruction())
                || !draft.getRiskFlags().isEmpty();
        if (!hasContent) return null;
        if (populateMeta) {
            if (!StringUtils.hasText(draft.getPromptVersion())) {
                draft.setPromptVersion(resolveDoctorHandlePromptVersion(null));
            }
            if (!StringUtils.hasText(draft.getSource())) {
                draft.setSource(Objects.equals(draft.getFallback(), 1) ? "fallback" : "deepseek");
            }
            if (draft.getFallback() == null) {
                draft.setFallback("fallback".equalsIgnoreCase(draft.getSource()) ? 1 : 0);
            }
        }
        return draft;
    }

    private DoctorConsultationFollowUpDraftVO generateAiFollowUpDraft(Doctor doctor,
                                                                      AdminConsultationRecordVO record,
                                                                      List<ConsultationMessageVO> messages,
                                                                      String targetField,
                                                                      String rewriteRequirement,
                                                                      String currentDraftJson) {
        if (doctor == null || record == null || !isDoctorAiAvailable()) return null;

        try {
            DeepSeekChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) return null;

            DoctorConsultationFollowUpDraftVO draft = ChatClient.create(chatModel)
                    .prompt()
                    .system(buildDoctorFollowUpSystemPrompt(targetField))
                    .user(buildDoctorFollowUpUserPrompt(doctor, record, messages, targetField, rewriteRequirement, currentDraftJson))
                    .call()
                    .entity(DoctorConsultationFollowUpDraftVO.class);
            draft = normalizeFollowUpDraft(draft, false);
            if (draft == null) return null;
            draft.setSource("deepseek");
            draft.setFallback(0);
            draft.setPromptVersion(resolveDoctorFollowUpPromptVersion(targetField));
            return draft;
        } catch (Exception exception) {
            log.warn("Doctor AI follow-up draft generation skipped for consultation {} because DeepSeek call failed: {}",
                    record.getId(), exception.getMessage());
            return null;
        }
    }

    private String buildDoctorFollowUpSystemPrompt(String targetField) {
        String targetRule = buildFollowUpTargetFieldSystemRule(targetField);
        return """
                你是互联网医疗平台中的医生随访助手。
                你正在帮助真实医生起草一份“随访记录表单”草稿，供医生人工修改后保存。
                你必须遵守以下规则：
                1. 只输出 JSON，不要输出 Markdown、代码块或解释过程。
                2. followUpType 只能返回 platform、phone、offline、other 之一。
                3. patientStatus 只能返回 improved、stable、worsened、other 之一。
                4. summary 字段写本次随访摘要，150 字以内。
                5. advice 字段写本次随访建议，240 字以内。
                6. nextStep 字段写下一步安排，没有可返回空字符串。
                7. needRevisit 只能返回 0 或 1；若为 1，nextFollowUpDate 必须返回 YYYY-MM-DD 格式日期，否则返回空字符串或 null。
                8. generationSummary 字段用一句话说明起草依据，便于医生快速判断是否采用。
                9. riskFlags 最多返回 4 条，只保留当前仍值得继续提醒的风险点。
                10. 如果患者恢复欠佳或存在高风险，需要在 advice 和 nextStep 中体现及时线下复诊或急诊提醒。
                11. 不要编造患者已经完成的检查、治疗结果或量化指标。
                12. 如果用户补充了当前草稿快照，请优先沿用其中已经确认的结构、语气和安排，除非上下文强烈提示必须调整。
                %s""".formatted(targetRule);
    }

    private String buildDoctorFollowUpUserPrompt(Doctor doctor,
                                                 AdminConsultationRecordVO record,
                                                 List<ConsultationMessageVO> messages,
                                                 String targetField,
                                                 String rewriteRequirement,
                                                 String currentDraftJson) {
        ConsultationMessageVO latestPatientMessage = latestPatientMessage(messages);
        List<String> riskFlags = extractRiskFlags(record);
        String conversationHistory = buildConversationHistory(messages);
        String followUpHistory = buildFollowUpHistorySummary(record);
        String rewriteContextBlock = buildFollowUpRewriteContextBlock(currentDraftJson, rewriteRequirement);

        String prompt = """
                请结合以下资料，生成一份医生可编辑的随访记录草稿。

                医生信息：
                - 医生：%s
                - 科室：%s
                - 职称：%s

                问诊信息：
                - 问诊ID：%s
                - 问诊单号：%s
                - 患者：%s
                - 当前状态：%s

                主诉：
                %s

                当前已保存医生处理：
                %s

                当前已保存结构化结论：
                %s

                历史随访记录：
                %s

                最近沟通记录：
                %s

                最近一条患者消息：
                %s

                当前风险提示：
                %s

                生成要求：
                - 优先围绕恢复情况、既往处理建议执行情况和是否需要继续复诊来生成。
                - 如果当前资料不足，不要假设患者已经恢复，可在 summary 和 advice 中体现“待进一步确认”。
                - 如果患者疑似加重或风险仍高，needRevisit 应优先返回 1，并明确更快线下复诊提醒。
                %s
                """.formatted(
                safeText(doctor.getName(), "-"),
                safeText(record.getDepartmentName(), "-"),
                safeText(doctor.getTitle(), "-"),
                safeNumber(record.getId()),
                safeText(record.getConsultationNo(), "-"),
                safeText(record.getPatientName(), "-"),
                safeText(record.getStatus(), "-"),
                safeText(record.getChiefComplaint(), "暂无主诉"),
                safeText(buildDoctorHandleSummary(record), "当前还没有医生处理记录"),
                safeText(buildDoctorConclusionSummary(record), "当前还没有结构化结论"),
                safeText(followUpHistory, "暂无历史随访记录"),
                safeText(conversationHistory, "暂无沟通记录"),
                safeText(latestPatientMessage == null ? null : latestPatientMessage.getContent(), "暂无患者新增反馈"),
                safeText(joinList(riskFlags, "、"), "暂无明确风险提示"),
                safeText(buildFollowUpTargetFieldUserHint(targetField), "无额外目标字段要求。")
        );
        return StringUtils.hasText(rewriteContextBlock) ? prompt + "\n\n" + rewriteContextBlock : prompt;
    }

    private String normalizeRewriteRequirement(String rewriteRequirement) {
        return abbreviate(trimToNull(rewriteRequirement), 500);
    }

    private String normalizeCurrentDraftJson(String currentDraftJson) {
        String normalized = trimToNull(currentDraftJson);
        if (normalized == null) return null;
        try {
            return abbreviate(JSON.toJSONString(JSON.parse(normalized)), 10000);
        } catch (Exception exception) {
            return abbreviate(normalized, 10000);
        }
    }

    private boolean hasDraftRewriteContext(String rewriteRequirement, String currentDraftJson) {
        return StringUtils.hasText(rewriteRequirement) || StringUtils.hasText(currentDraftJson);
    }

    private String buildHandleRewriteContextBlock(String currentDraftJson, String rewriteRequirement) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(currentDraftJson)) {
            segments.add("""
                    当前草稿快照（JSON）：
                    %s

                    读取说明：
                    - editingDraft 是医生当前正在编辑的处理表单内容。
                    - conclusionDraft 是医生当前正在编辑的结构化结论。
                    - currentAiDraft 是上一版 AI 处理草稿。
                    - 如果这些草稿里已有明确内容，请优先沿用其结构、语气和已确认判断，只改必要部分。
                    """.formatted(currentDraftJson));
        }
        if (StringUtils.hasText(rewriteRequirement)) {
            segments.add("医生补充改写要求：" + rewriteRequirement);
        }
        return segments.isEmpty() ? null : String.join("\n\n", segments);
    }

    private String buildFollowUpRewriteContextBlock(String currentDraftJson, String rewriteRequirement) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(currentDraftJson)) {
            segments.add("""
                    当前草稿快照（JSON）：
                    %s

                    读取说明：
                    - followUpDraft 是医生当前正在编辑的随访表单内容。
                    - currentAiDraft 是上一版 AI 随访草稿。
                    - 如果这些草稿里已有明确内容，请优先沿用其结构、语气和已确认安排，只改必要部分。
                    """.formatted(currentDraftJson));
        }
        if (StringUtils.hasText(rewriteRequirement)) {
            segments.add("医生补充改写要求：" + rewriteRequirement);
        }
        return segments.isEmpty() ? null : String.join("\n\n", segments);
    }

    private DoctorConsultationFollowUpDraftVO buildFallbackFollowUpDraft(AdminConsultationRecordVO record,
                                                                         List<ConsultationMessageVO> messages,
                                                                         String targetField) {
        ConsultationDoctorFollowUpVO latestFollowUp = latestDoctorFollowUp(record);
        ConsultationMessageVO latestPatientMessage = latestPatientMessage(messages);
        List<String> riskFlags = extractRiskFlags(record);
        String patientStatus = resolvePatientStatus(latestPatientMessage, latestFollowUp);
        boolean highRisk = "worsened".equals(patientStatus) || isRiskConsultation(record) || !riskFlags.isEmpty();
        Integer needRevisit = resolveNeedRevisit(record, patientStatus, highRisk);
        String nextFollowUpDate = needRevisit == 1 ? resolveNextFollowUpDateText(record, latestFollowUp, patientStatus) : null;

        List<String> summarySegments = new ArrayList<>();
        addSegment(summarySegments, "前次随访", latestFollowUp == null ? null : latestFollowUp.getSummary());
        if (latestPatientMessage != null) {
            summarySegments.add("患者最新反馈：" + abbreviate(latestPatientMessage.getContent(), 120));
        }
        if (summarySegments.isEmpty()) {
            summarySegments.add("本轮继续围绕前次处理建议后的恢复情况进行随访观察。");
        }

        List<String> adviceSegments = new ArrayList<>();
        addSegment(adviceSegments, "前次建议", latestFollowUp == null ? null : latestFollowUp.getAdvice());
        addSegment(adviceSegments, "处理建议", record.getDoctorHandle() == null ? null : record.getDoctorHandle().getMedicalAdvice());
        if (highRisk) {
            adviceSegments.add("如症状持续加重或出现风险信号，建议尽快线下复诊，必要时直接急诊。");
        } else {
            adviceSegments.add("建议继续按照既往处理方案观察恢复情况，并关注主要不适是否继续缓解。");
        }

        List<String> nextStepSegments = new ArrayList<>();
        addSegment(nextStepSegments, "既往安排", latestFollowUp == null ? null : latestFollowUp.getNextStep());
        if (needRevisit == 1) {
            nextStepSegments.add("建议于 " + nextFollowUpDate + " 前继续完成下一轮随访，如中途症状明显变化可提前复诊。");
        } else {
            nextStepSegments.add("当前可继续按既定建议观察，如症状反复或加重再提前联系医生。");
        }

        DoctorConsultationFollowUpDraftVO draft = new DoctorConsultationFollowUpDraftVO();
        draft.setFollowUpType(resolveFollowUpType(latestFollowUp));
        draft.setPatientStatus(patientStatus);
        draft.setSummary(abbreviate(String.join("；", summarySegments), 500));
        draft.setAdvice(abbreviate(String.join("；", adviceSegments), 1000));
        draft.setNextStep(abbreviate(String.join("；", nextStepSegments), 500));
        draft.setNeedRevisit(needRevisit);
        draft.setNextFollowUpDate(nextFollowUpDate);
        draft.setGenerationSummary(resolveTargetedGenerationSummary("已结合前次处理结果、历史随访和最近患者反馈生成随访草稿。", renderFollowUpDraftFieldLabel(targetField)));
        draft.setRiskFlags(riskFlags);
        draft.setPromptVersion(resolveDoctorFollowUpPromptVersion(targetField));
        draft.setSource("fallback");
        draft.setFallback(1);
        return draft;
    }

    private DoctorConsultationFollowUpDraftVO normalizeFollowUpDraft(DoctorConsultationFollowUpDraftVO draft, boolean populateMeta) {
        if (draft == null) return null;
        draft.setFollowUpType(normalizeFollowUpType(draft.getFollowUpType()));
        draft.setPatientStatus(normalizePatientStatus(draft.getPatientStatus()));
        draft.setSummary(abbreviate(trimToNull(draft.getSummary()), 500));
        draft.setAdvice(abbreviate(trimToNull(draft.getAdvice()), 1000));
        draft.setNextStep(abbreviate(trimToNull(draft.getNextStep()), 500));
        draft.setNeedRevisit(normalizeBinaryFlag(draft.getNeedRevisit()));
        if (draft.getNeedRevisit() == 1) {
            draft.setNextFollowUpDate(normalizeNextFollowUpDateText(draft.getNextFollowUpDate(), 3));
        } else {
            draft.setNextFollowUpDate(null);
        }
        draft.setGenerationSummary(abbreviate(trimToNull(draft.getGenerationSummary()), 160));
        draft.setRiskFlags(normalizeTextList(draft.getRiskFlags(), 4, 30));
        boolean hasContent = StringUtils.hasText(draft.getSummary())
                || StringUtils.hasText(draft.getAdvice())
                || StringUtils.hasText(draft.getNextStep())
                || !draft.getRiskFlags().isEmpty();
        if (!hasContent) return null;
        if (populateMeta) {
            if (!StringUtils.hasText(draft.getPromptVersion())) {
                draft.setPromptVersion(resolveDoctorFollowUpPromptVersion(null));
            }
            if (!StringUtils.hasText(draft.getSource())) {
                draft.setSource(Objects.equals(draft.getFallback(), 1) ? "fallback" : "deepseek");
            }
            if (draft.getFallback() == null) {
                draft.setFallback("fallback".equalsIgnoreCase(draft.getSource()) ? 1 : 0);
            }
        }
        return draft;
    }

    private DoctorConsultationMessageDraftVO generateAiMessageDraft(Doctor doctor,
                                                                    AdminConsultationRecordVO record,
                                                                    List<ConsultationMessageVO> messages,
                                                                    String sceneType) {
        if (doctor == null || record == null || !isDoctorAiAvailable()) return null;

        try {
            DeepSeekChatModel chatModel = chatModelProvider.getIfAvailable();
            if (chatModel == null) return null;

            DoctorConsultationMessageDraftVO draft = ChatClient.create(chatModel)
                    .prompt()
                    .system(buildDoctorMessageSystemPrompt(sceneType))
                    .user(buildDoctorMessageUserPrompt(doctor, record, messages, sceneType))
                    .call()
                    .entity(DoctorConsultationMessageDraftVO.class);
            draft = normalizeMessageDraft(draft, false);
            if (draft == null || !StringUtils.hasText(draft.getContent())) return null;
            draft.setSource("deepseek");
            draft.setFallback(0);
            draft.setSceneType(sceneType);
            draft.setPromptVersion(resolveDoctorMessagePromptVersion(sceneType));
            return draft;
        } catch (Exception exception) {
            log.warn("Doctor AI message draft generation skipped for consultation {} because DeepSeek call failed: {}",
                    record.getId(), exception.getMessage());
            return null;
        }
    }

    private String buildDoctorMessageSystemPrompt(String sceneType) {
        return """
                你是互联网医疗平台中的医生接诊助手。
                你正在帮助真实医生起草一段准备发给患者的沟通消息，供医生人工修改后发送。
                你必须遵守以下规则：
                1. 只输出 JSON，不要输出 Markdown、代码块或解释过程。
                2. content 字段直接写给患者，语气专业、清晰、安抚，不要提到 AI 或模型，不要给出确定性诊断。
                3. 如果当前资料不足，优先提出 2 到 3 个最关键的补充问题。
                4. 如果存在明显高风险，content 中要明确建议尽快线下就医或立即急诊。
                5. summary 字段用一句话说明起草依据，供医生快速判断是否采用。
                6. riskFlags 最多返回 4 条，只保留最值得医生关注的风险信号。
                7. content 控制在 220 字以内，适合直接发给患者。
                8. 当前沟通场景为：%s，请按该场景组织回复重点。
                """.formatted(renderMessageDraftSceneLabel(sceneType));
    }

    private String buildDoctorMessageUserPrompt(Doctor doctor,
                                                AdminConsultationRecordVO record,
                                                List<ConsultationMessageVO> messages,
                                                String sceneType) {
        JSONObject aiPayload = latestAiStructuredPayload(record);
        ConsultationMessageVO latestPatientMessage = latestPatientMessage(messages);
        String answerSummary = buildAnswerSummary(record);
        String conversationHistory = buildConversationHistory(messages);
        String riskFlags = joinList(extractRiskFlags(record), "、");
        String nextQuestions = joinList(extractNextQuestions(aiPayload), "；");
        String handleSummary = buildDoctorHandleSummary(record);
        String aiReply = firstText(
                trimToNull(aiPayload == null ? null : aiPayload.getString("reply")),
                latestAiReplyText(record)
        );

        return """
                请为当前问诊生成一段医生可编辑的沟通回复草稿。

                医生信息：
                - 医生：%s
                - 科室：%s
                - 职称：%s

                问诊信息：
                - 问诊ID：%s
                - 问诊单号：%s
                - 患者：%s
                - 问诊分类：%s
                - 当前状态：%s
                - 当前分诊等级：%s
                - 当前建议动作：%s

                主诉：
                %s

                健康摘要：
                %s

                问诊答案摘要：
                %s

                AI/规则导诊摘要：
                - 导诊建议：%s
                - AI 回复参考：%s
                - 风险提示：%s
                - AI 建议补充：%s

                医生当前处理进度：
                %s

                最近沟通记录：
                %s

                最近一条患者消息：
                %s

                当前沟通场景：
                - 场景：%s
                - 场景说明：%s

                生成要求：
                - 如果最近一条消息来自患者，请优先回应这条消息。
                - 如果还没有患者新消息，就生成一段“已接手 + 下一步建议”的开场回复。
                - 如存在高风险，请把线下就医或急诊提醒写清楚。
                - 如信息不足，请在回复中自然地补充关键追问。
                """.formatted(
                safeText(doctor.getName(), "-"),
                safeText(record.getDepartmentName(), "-"),
                safeText(doctor.getTitle(), "-"),
                safeNumber(record.getId()),
                safeText(record.getConsultationNo(), "-"),
                safeText(record.getPatientName(), "-"),
                safeText(record.getCategoryName(), "-"),
                safeText(record.getStatus(), "-"),
                safeText(record.getTriageLevelName(), "待评估"),
                safeText(renderVisitType(record.getTriageActionType()), "继续观察"),
                safeText(record.getChiefComplaint(), "暂无主诉"),
                safeText(record.getHealthSummary(), "暂无健康摘要"),
                safeText(answerSummary, "暂无问诊答案"),
                safeText(firstText(trimToNull(record.getTriageSuggestion()), trimToNull(record.getTriageRuleSummary())), "暂无导诊建议"),
                safeText(aiReply, "暂无 AI 继续回复"),
                safeText(riskFlags, "暂无明确高风险提示"),
                safeText(nextQuestions, "暂无额外补充问题"),
                safeText(handleSummary, "当前还没有医生处理记录"),
                safeText(conversationHistory, "暂无沟通记录"),
                safeText(latestPatientMessage == null ? null : latestPatientMessage.getContent(), "暂无患者新消息"),
                renderMessageDraftSceneLabel(sceneType),
                renderMessageDraftSceneHint(sceneType)
        );
    }

    private DoctorConsultationMessageDraftVO buildFallbackMessageDraft(Doctor doctor,
                                                                       AdminConsultationRecordVO record,
                                                                       List<ConsultationMessageVO> messages,
                                                                       String sceneType) {
        DoctorConsultationMessageDraftVO draft = new DoctorConsultationMessageDraftVO();
        List<String> riskFlags = extractRiskFlags(record);
        JSONObject aiPayload = latestAiStructuredPayload(record);
        ConsultationMessageVO latestPatientMessage = latestPatientMessage(messages);
        boolean highRisk = isRiskConsultation(record) || !riskFlags.isEmpty();

        List<String> segments = new ArrayList<>();
        segments.add(resolveFallbackSceneOpening(sceneType, doctor, messages));

        if (highRisk) {
            String direction = "emergency".equalsIgnoreCase(trimToNull(record.getTriageActionType()))
                    ? "建议优先直接急诊处理，不建议继续等待。"
                    : "结合目前描述，建议尽快线下就医进一步评估。";
            segments.add(direction);
        } else {
            String aiReply = firstText(
                    trimToNull(aiPayload == null ? null : aiPayload.getString("reply")),
                    trimToNull(record.getTriageSuggestion())
            );
            if (aiReply != null) {
                segments.add(abbreviate(aiReply, 120));
            } else {
                segments.add(resolveFallbackSceneAdvice(sceneType));
            }
        }

        if (!highRisk) {
            List<String> nextQuestions = extractNextQuestions(aiPayload);
            if (!nextQuestions.isEmpty()) {
                segments.add("方便的话请再补充：" + joinList(nextQuestions.stream().limit(2).toList(), "；") + "。");
            } else if (latestPatientMessage == null || "follow_up".equals(sceneType)) {
                segments.add("如果方便，也请补充症状持续时间、是否加重，以及是否已经自行用药。");
            }
        }

        if (!riskFlags.isEmpty()) {
            segments.add("如出现" + joinList(riskFlags.stream().limit(3).toList(), "、") + "等情况，请及时线下就医。");
        }

        draft.setContent(abbreviate(joinSegments(segments), 220));
        draft.setSummary(highRisk
                ? renderMessageDraftSceneLabel(sceneType) + "场景下检测到高风险信号，已优先生成线下就医提醒。"
                : "已按" + renderMessageDraftSceneLabel(sceneType) + "场景生成一段医生可编辑回复。");
        draft.setRiskFlags(riskFlags);
        draft.setSceneType(sceneType);
        draft.setPromptVersion(resolveDoctorMessagePromptVersion(sceneType));
        draft.setSource("fallback");
        draft.setFallback(1);
        return draft;
    }

    private DoctorConsultationMessageDraftVO normalizeMessageDraft(DoctorConsultationMessageDraftVO draft, boolean populateMeta) {
        if (draft == null) return null;
        draft.setContent(abbreviate(trimToNull(draft.getContent()), 260));
        draft.setSummary(abbreviate(trimToNull(draft.getSummary()), 160));
        draft.setRiskFlags(normalizeTextList(draft.getRiskFlags(), 4, 30));
        draft.setSceneType(normalizeMessageDraftSceneType(draft.getSceneType(), null, null));
        if (!StringUtils.hasText(draft.getContent()) && !StringUtils.hasText(draft.getSummary()) && draft.getRiskFlags().isEmpty()) {
            return null;
        }
        if (populateMeta) {
            if (!StringUtils.hasText(draft.getPromptVersion())) {
                draft.setPromptVersion(resolveDoctorMessagePromptVersion(draft.getSceneType()));
            }
            if (!StringUtils.hasText(draft.getSource())) {
                draft.setSource(Objects.equals(draft.getFallback(), 1) ? "fallback" : "deepseek");
            }
            if (draft.getFallback() == null) {
                draft.setFallback("fallback".equalsIgnoreCase(draft.getSource()) ? 1 : 0);
            }
        }
        return draft;
    }

    private DoctorFormAiLog saveDoctorFormAiLog(Doctor doctor,
                                                AdminConsultationRecordVO record,
                                                String sceneType,
                                                String regenerateField,
                                                String source,
                                                String promptVersion,
                                                Integer fallback,
                                                List<String> riskFlags,
                                                String draftSummary,
                                                boolean draftContextUsed,
                                                String rewriteRequirement,
                                                String draftPreview,
                                                String draftPayloadJson) {
        if (doctor == null || record == null) return null;
        Date now = new Date();
        DoctorFormAiLog logRecord = new DoctorFormAiLog(
                null,
                record.getId(),
                abbreviate(trimToNull(record.getConsultationNo()), 50),
                abbreviate(trimToNull(record.getPatientName()), 50),
                abbreviate(trimToNull(record.getCategoryName()), 100),
                doctor.getId(),
                abbreviate(trimToNull(doctor.getName()), 50),
                doctor.getDepartmentId(),
                abbreviate(trimToNull(record.getDepartmentName()), 100),
                normalizeFormDraftSceneType(sceneType),
                normalizeFormDraftRegenerateField(sceneType, regenerateField),
                abbreviate(trimToNull(source), 20),
                abbreviate(trimToNull(promptVersion), 100),
                Objects.equals(fallback, 1) ? 1 : 0,
                buildRiskFlagsJson(riskFlags),
                abbreviate(trimToNull(draftSummary), 255),
                draftContextUsed ? 1 : 0,
                abbreviate(trimToNull(rewriteRequirement), 500),
                abbreviate(trimToNull(draftPreview), 1000),
                trimToNull(draftPayloadJson),
                0,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                null,
                now,
                null,
                null,
                now,
                now
        );
        return doctorFormAiLogMapper.insert(logRecord) > 0 ? logRecord : null;
    }

    private void markDoctorFormAiLogSaved(Integer aiLogId,
                                          Doctor doctor,
                                          ConsultationRecord record,
                                          String savedTarget,
                                          String savedPreview,
                                          String savedPayloadJson,
                                          Date now) {
        if (aiLogId == null || doctor == null || record == null || now == null) return;
        DoctorFormAiLog logRecord = doctorFormAiLogMapper.selectById(aiLogId);
        if (logRecord == null) return;
        if (!Objects.equals(logRecord.getDoctorId(), doctor.getId())) return;
        if (!Objects.equals(logRecord.getConsultationId(), record.getId())) return;
        if (!normalizeFormDraftSceneType(savedTarget).equalsIgnoreCase(trimToNull(logRecord.getSceneType()))) return;

        logRecord.setSavedStatus(1);
        logRecord.setSavedTarget(normalizeFormDraftSceneType(savedTarget));
        logRecord.setSavedPreview(abbreviate(trimToNull(savedPreview), 1000));
        logRecord.setSavedPayloadJson(trimToNull(savedPayloadJson));
        logRecord.setSavedTime(now);
        logRecord.setUpdateTime(now);
        doctorFormAiLogMapper.updateById(logRecord);
    }

    private String buildHandleDraftPreview(DoctorConsultationHandleDraftVO draft) {
        if (draft == null) return null;
        List<String> segments = new ArrayList<>();
        addSegment(segments, "判断摘要", draft.getDoctorSummary());
        addSegment(segments, "处理建议", draft.getMedicalAdvice());
        addSegment(segments, "随访计划", draft.getFollowUpPlan());
        addSegment(segments, "患者提示", draft.getPatientInstruction());
        return String.join("；", segments);
    }

    private String buildFollowUpDraftPreview(DoctorConsultationFollowUpDraftVO draft) {
        if (draft == null) return null;
        List<String> segments = new ArrayList<>();
        addSegment(segments, "随访摘要", draft.getSummary());
        addSegment(segments, "随访建议", draft.getAdvice());
        addSegment(segments, "下一步", draft.getNextStep());
        return String.join("；", segments);
    }

    private String buildSavedHandlePreview(DoctorConsultationHandleSubmitVO vo) {
        if (vo == null) return null;
        List<String> segments = new ArrayList<>();
        addSegment(segments, "判断摘要", vo.getSummary());
        addSegment(segments, "处理建议", vo.getMedicalAdvice());
        addSegment(segments, "随访计划", vo.getFollowUpPlan());
        addSegment(segments, "患者提示", vo.getPatientInstruction());
        return String.join("；", segments);
    }

    private String buildSavedHandlePayload(DoctorConsultationHandleSubmitVO vo) {
        if (vo == null) return null;
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", trimToNull(vo.getStatus()));
        payload.put("summary", trimToNull(vo.getSummary()));
        payload.put("medicalAdvice", trimToNull(vo.getMedicalAdvice()));
        payload.put("followUpPlan", trimToNull(vo.getFollowUpPlan()));
        payload.put("internalRemark", trimToNull(vo.getInternalRemark()));
        payload.put("conditionLevel", trimToNull(vo.getConditionLevel()));
        payload.put("disposition", trimToNull(vo.getDisposition()));
        payload.put("diagnosisDirection", trimToNull(vo.getDiagnosisDirection()));
        payload.put("conclusionTags", normalizeConclusionTags(vo.getConclusionTags()));
        payload.put("needFollowUp", vo.getNeedFollowUp());
        payload.put("followUpWithinDays", vo.getFollowUpWithinDays());
        payload.put("isConsistentWithAi", vo.getIsConsistentWithAi());
        payload.put("aiMismatchReasons", ConsultationAiMismatchReasonUtils.normalizeCodes(vo.getAiMismatchReasons()));
        payload.put("aiMismatchRemark", trimToNull(vo.getAiMismatchRemark()));
        payload.put("patientInstruction", trimToNull(vo.getPatientInstruction()));
        payload.put("checkSuggestions", vo.getCheckSuggestions() == null ? List.of() : vo.getCheckSuggestions().stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("itemName", trimToNull(item.getItemName()));
                    suggestion.put("itemType", normalizeCheckItemType(item.getItemType()));
                    suggestion.put("urgencyLevel", normalizeCheckUrgencyLevel(item.getUrgencyLevel()));
                    suggestion.put("purpose", trimToNull(item.getPurpose()));
                    suggestion.put("attentionNote", trimToNull(item.getAttentionNote()));
                    return suggestion;
                })
                .filter(item -> item.get("itemName") != null
                        || item.get("purpose") != null
                        || item.get("attentionNote") != null)
                .toList());
        payload.put("prescriptions", vo.getPrescriptions() == null ? List.of() : vo.getPrescriptions().stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    Map<String, Object> prescription = new HashMap<>();
                    prescription.put("medicineId", item.getMedicineId());
                    prescription.put("dosage", trimToNull(item.getDosage()));
                    prescription.put("frequency", trimToNull(item.getFrequency()));
                    prescription.put("durationDays", item.getDurationDays());
                    prescription.put("medicationInstruction", trimToNull(item.getMedicationInstruction()));
                    return prescription;
                })
                .filter(item -> item.get("medicineId") != null
                        || item.get("dosage") != null
                        || item.get("frequency") != null
                        || item.get("durationDays") != null
                        || item.get("medicationInstruction") != null)
                .toList());
        return JSON.toJSONString(payload);
    }

    private List<ConsultationCheckSuggestionVO> listCheckSuggestions(int consultationId) {
        return consultationCheckSuggestionMapper.selectList(Wrappers.<ConsultationCheckSuggestion>query()
                        .eq("consultation_id", consultationId)
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationCheckSuggestionVO.class))
                .toList();
    }

    private List<ConsultationReportFeedbackVO> listReportFeedbacks(int consultationId) {
        return consultationReportFeedbackMapper.selectList(Wrappers.<ConsultationReportFeedback>query()
                        .eq("consultation_id", consultationId)
                        .eq("status", 1)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationReportFeedbackVO.class, vo ->
                        vo.setAttachments(parseJsonStringList(item.getAttachmentsJson()))))
                .toList();
    }

    private List<ConsultationMedicationFeedbackVO> listMedicationFeedbacks(int consultationId) {
        return consultationMedicationFeedbackMapper.selectList(Wrappers.<ConsultationMedicationFeedback>query()
                        .eq("consultation_id", consultationId)
                        .eq("status", 1)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationMedicationFeedbackVO.class, vo ->
                        vo.setAttachments(parseJsonStringList(item.getAttachmentsJson()))))
                .toList();
    }

    private String replaceConsultationCheckSuggestions(int consultationId,
                                                       Doctor doctor,
                                                       String departmentName,
                                                       List<DoctorCheckSuggestionItemSubmitVO> items,
                                                       Date now) {
        if (items == null) return null;

        List<ConsultationCheckSuggestion> entities = new ArrayList<>();
        int sort = 0;
        for (int index = 0; index < items.size(); index++) {
            DoctorCheckSuggestionItemSubmitVO item = items.get(index);
            if (item == null) continue;
            String itemName = trimToNull(item.getItemName());
            String itemType = normalizeCheckItemType(item.getItemType());
            String urgencyLevel = normalizeCheckUrgencyLevel(item.getUrgencyLevel());
            String purpose = abbreviate(trimToNull(item.getPurpose()), 300);
            String attentionNote = abbreviate(trimToNull(item.getAttentionNote()), 300);
            if (itemName == null && itemType == null && urgencyLevel == null && purpose == null && attentionNote == null) {
                continue;
            }
            if (itemName == null) return "请填写第 " + (index + 1) + " 条检查建议名称";
            if (itemType == null) return "请选择第 " + (index + 1) + " 条检查建议类型";
            if (urgencyLevel == null) return "请选择第 " + (index + 1) + " 条检查建议紧急程度";
            entities.add(new ConsultationCheckSuggestion(
                    null,
                    consultationId,
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getDepartmentId(),
                    departmentName,
                    itemName,
                    itemType,
                    urgencyLevel,
                    purpose,
                    attentionNote,
                    1,
                    sort++,
                    now,
                    now
            ));
        }

        consultationCheckSuggestionMapper.delete(Wrappers.<ConsultationCheckSuggestion>query()
                .eq("consultation_id", consultationId));
        for (ConsultationCheckSuggestion entity : entities) {
            if (consultationCheckSuggestionMapper.insert(entity) <= 0) {
                return "检查建议保存失败";
            }
        }
        return null;
    }

    private String normalizeCheckItemType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("lab", "imaging", "pathology", "other").contains(normalized) ? normalized : null;
    }

    private String normalizeCheckUrgencyLevel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("routine", "soon", "urgent").contains(normalized) ? normalized : null;
    }

    private List<String> parseJsonStringList(String json) {
        String text = trimToNull(json);
        if (text == null) return List.of();
        try {
            List<String> items = JSON.parseArray(text, String.class);
            if (items == null) return List.of();
            return items.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ignored) {
            return List.of(text);
        }
    }

    private String buildSavedFollowUpPreview(ConsultationDoctorFollowUp followUp) {
        if (followUp == null) return null;
        List<String> segments = new ArrayList<>();
        addSegment(segments, "随访摘要", followUp.getSummary());
        addSegment(segments, "随访建议", followUp.getAdvice());
        addSegment(segments, "下一步", followUp.getNextStep());
        return String.join("；", segments);
    }

    private String buildSavedFollowUpPayload(ConsultationDoctorFollowUp followUp) {
        if (followUp == null) return null;
        Map<String, Object> payload = new HashMap<>();
        payload.put("followUpType", trimToNull(followUp.getFollowUpType()));
        payload.put("patientStatus", trimToNull(followUp.getPatientStatus()));
        payload.put("summary", trimToNull(followUp.getSummary()));
        payload.put("advice", trimToNull(followUp.getAdvice()));
        payload.put("nextStep", trimToNull(followUp.getNextStep()));
        payload.put("needRevisit", followUp.getNeedRevisit());
        payload.put("nextFollowUpDate", formatDateText(followUp.getNextFollowUpDate()));
        return JSON.toJSONString(payload);
    }

    private DoctorMessageAiLog saveDoctorMessageAiLog(Doctor doctor,
                                                      AdminConsultationRecordVO record,
                                                      DoctorConsultationMessageDraftVO draft) {
        if (doctor == null || record == null || draft == null) return null;
        Date now = new Date();
        DoctorMessageAiLog logRecord = new DoctorMessageAiLog(
                null,
                record.getId(),
                abbreviate(trimToNull(record.getConsultationNo()), 50),
                abbreviate(trimToNull(record.getPatientName()), 50),
                abbreviate(trimToNull(record.getCategoryName()), 100),
                doctor.getId(),
                abbreviate(trimToNull(doctor.getName()), 50),
                doctor.getDepartmentId(),
                abbreviate(trimToNull(record.getDepartmentName()), 100),
                normalizeMessageDraftSceneType(draft.getSceneType(), record, null),
                abbreviate(trimToNull(draft.getSource()), 20),
                abbreviate(trimToNull(draft.getPromptVersion()), 100),
                Objects.equals(draft.getFallback(), 1) ? 1 : 0,
                buildRiskFlagsJson(draft.getRiskFlags()),
                abbreviate(trimToNull(draft.getSummary()), 255),
                abbreviate(trimToNull(draft.getContent()), 1000),
                0,
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                now,
                null,
                null,
                now,
                now
        );
        return doctorMessageAiLogMapper.insert(logRecord) > 0 ? logRecord : null;
    }

    private String buildRiskFlagsJson(List<String> riskFlags) {
        List<String> normalized = normalizeTextList(riskFlags, 6, 30);
        return normalized.isEmpty() ? null : JSON.toJSONString(normalized);
    }

    private boolean hasTemplateUsage(String templateSceneType, Integer templateId, String templateTitle) {
        return templateId != null
                || trimToNull(templateSceneType) != null
                || trimToNull(templateTitle) != null;
    }

    private String normalizeFormDraftSceneType(String sceneType) {
        String normalized = trimToNull(sceneType);
        if (normalized == null) return "handle";
        normalized = normalized.toLowerCase();
        return List.of("handle", "follow_up").contains(normalized) ? normalized : "handle";
    }

    private String normalizeFormDraftRegenerateField(String sceneType, String fieldKey) {
        String normalizedSceneType = normalizeFormDraftSceneType(sceneType);
        return "follow_up".equals(normalizedSceneType)
                ? normalizeFollowUpDraftRegenerateField(fieldKey)
                : normalizeHandleDraftRegenerateField(fieldKey);
    }

    private String normalizeFormDraftApplyTarget(String applyTarget) {
        String normalized = trimToNull(applyTarget);
        if (normalized == null) return "handle_form";
        normalized = normalized.toLowerCase();
        return List.of("handle_form", "conclusion_form", "follow_up_form").contains(normalized)
                ? normalized
                : "handle_form";
    }

    private String normalizeFormTemplateSceneType(String sceneType) {
        String normalized = trimToNull(sceneType);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("handle_summary", "medical_advice", "follow_up_plan", "patient_instruction",
                        "followup_summary", "followup_advice", "followup_next_step").contains(normalized)
                ? normalized
                : null;
    }

    private String normalizeHandleDraftRegenerateField(String fieldKey) {
        String normalized = trimToNull(fieldKey);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("doctor_summary", "medical_advice", "follow_up_plan", "patient_instruction").contains(normalized)
                ? normalized
                : null;
    }

    private String normalizeFollowUpDraftRegenerateField(String fieldKey) {
        String normalized = trimToNull(fieldKey);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("followup_summary", "followup_advice", "followup_next_step").contains(normalized)
                ? normalized
                : null;
    }

    private String renderHandleDraftFieldLabel(String fieldKey) {
        String normalized = normalizeHandleDraftRegenerateField(fieldKey);
        if (normalized == null) return null;
        return switch (normalized) {
            case "medical_advice" -> "处理建议";
            case "follow_up_plan" -> "随访安排";
            case "patient_instruction" -> "患者提示";
            default -> "判断摘要";
        };
    }

    private String renderFollowUpDraftFieldLabel(String fieldKey) {
        String normalized = normalizeFollowUpDraftRegenerateField(fieldKey);
        if (normalized == null) return null;
        return switch (normalized) {
            case "followup_advice" -> "随访建议";
            case "followup_next_step" -> "下一步安排";
            default -> "随访摘要";
        };
    }

    private String buildHandleTargetFieldSystemRule(String targetField) {
        String fieldLabel = renderHandleDraftFieldLabel(targetField);
        if (fieldLabel == null) return "";
        return """
                16. 本次需要重点重写 %s，请优先让该字段更贴合当前问诊上下文、表达更具体。
                17. 其他字段仍需返回；除非上下文强烈提示需要联动调整，否则不要做无关的大幅改写。
                """.formatted(fieldLabel);
    }

    private String buildFollowUpTargetFieldSystemRule(String targetField) {
        String fieldLabel = renderFollowUpDraftFieldLabel(targetField);
        if (fieldLabel == null) return "";
        return """
                12. 本次需要重点重写 %s，请优先让该字段更贴合最新患者反馈和既往处理安排。
                13. 其他字段仍需返回；除非上下文强烈提示需要联动调整，否则不要做无关的大幅改写。
                """.formatted(fieldLabel);
    }

    private String buildHandleTargetFieldUserHint(String targetField) {
        String fieldLabel = renderHandleDraftFieldLabel(targetField);
        if (fieldLabel == null) return null;
        return """
                - 本次重点重写字段：%s
                - 请优先把该字段写得更具体、更可执行。
                - 其他字段继续返回，但只在确有必要时再同步调整。
                """.formatted(fieldLabel);
    }

    private String buildFollowUpTargetFieldUserHint(String targetField) {
        String fieldLabel = renderFollowUpDraftFieldLabel(targetField);
        if (fieldLabel == null) return null;
        return """
                - 本次重点重写字段：%s
                - 请优先围绕最新恢复情况把该字段写得更具体、更可执行。
                - 其他字段继续返回，但只在确有必要时再同步调整。
                """.formatted(fieldLabel);
    }

    private String resolveTargetedGenerationSummary(String summary, String fieldLabel) {
        String normalizedSummary = trimToNull(summary);
        if (fieldLabel == null) {
            return abbreviate(normalizedSummary, 160);
        }
        if (normalizedSummary != null && normalizedSummary.contains(fieldLabel)) {
            return abbreviate(normalizedSummary, 160);
        }
        String prefix = "本次聚焦重写" + fieldLabel + "。";
        if (normalizedSummary == null) {
            return abbreviate(prefix + "其余字段已按当前问诊上下文同步刷新。", 160);
        }
        return abbreviate(prefix + normalizedSummary, 160);
    }

    private String normalizeDraftApplyMode(String applyMode) {
        String normalized = trimToNull(applyMode);
        if (normalized == null) return "replace";
        normalized = normalized.toLowerCase();
        return List.of("replace", "append", "compose").contains(normalized) ? normalized : "replace";
    }

    private String normalizeTemplateSceneType(String sceneType) {
        String normalized = trimToNull(sceneType);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("message_opening", "message_clarify", "message_check_result", "message_follow_up").contains(normalized)
                ? normalized
                : null;
    }

    private boolean isDoctorAiAvailable() {
        return aiTriageProperties.isEnabled()
                && hasApiKey()
                && chatModelProvider.getIfAvailable() != null;
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(environment.getProperty("spring.ai.deepseek.api-key"))
                || StringUtils.hasText(environment.getProperty("spring.ai.deepseek.chat.api-key"));
    }

    private String normalizeMessageDraftSceneType(String sceneType,
                                                  AdminConsultationRecordVO record,
                                                  List<ConsultationMessageVO> messages) {
        String normalized = trimToNull(sceneType);
        if (normalized != null) {
            normalized = normalized.toLowerCase();
            if (List.of("opening", "clarify", "check_result", "follow_up").contains(normalized)) {
                return normalized;
            }
        }
        if (record != null && "completed".equals(record.getStatus())) {
            return "follow_up";
        }
        boolean hasConversation = messages != null && messages.stream().anyMatch(item ->
                item != null && ("user".equals(item.getSenderType()) || "doctor".equals(item.getSenderType())));
        return hasConversation ? "clarify" : "opening";
    }

    private String resolveDoctorMessagePromptVersion(String sceneType) {
        return (StringUtils.hasText(aiTriageProperties.getPromptVersion())
                ? aiTriageProperties.getPromptVersion().trim()
                : "deepseek-triage-v1") + "/doctor-message-" + normalizeMessageDraftSceneType(sceneType, null, null) + "-v1";
    }

    private String resolveDoctorHandlePromptVersion() {
        return resolveDoctorHandlePromptVersion(null);
    }

    private String resolveDoctorHandlePromptVersion(String targetField) {
        String normalizedField = normalizeHandleDraftRegenerateField(targetField);
        String suffix = normalizedField == null ? "doctor-handle-v2" : "doctor-handle-" + normalizedField + "-v2";
        return (StringUtils.hasText(aiTriageProperties.getPromptVersion())
                ? aiTriageProperties.getPromptVersion().trim()
                : "deepseek-triage-v1") + "/" + suffix;
    }

    private String resolveDoctorFollowUpPromptVersion() {
        return resolveDoctorFollowUpPromptVersion(null);
    }

    private String resolveDoctorFollowUpPromptVersion(String targetField) {
        String normalizedField = normalizeFollowUpDraftRegenerateField(targetField);
        String suffix = normalizedField == null ? "doctor-follow-up-v2" : "doctor-follow-up-" + normalizedField + "-v2";
        return (StringUtils.hasText(aiTriageProperties.getPromptVersion())
                ? aiTriageProperties.getPromptVersion().trim()
                : "deepseek-triage-v1") + "/" + suffix;
    }

    private String renderMessageDraftSceneLabel(String sceneType) {
        return switch (normalizeMessageDraftSceneType(sceneType, null, null)) {
            case "clarify" -> "补充追问";
            case "check_result" -> "结果解读";
            case "follow_up" -> "复诊随访";
            default -> "首次接诊";
        };
    }

    private String renderMessageDraftSceneHint(String sceneType) {
        return switch (normalizeMessageDraftSceneType(sceneType, null, null)) {
            case "clarify" -> "优先回应患者刚补充的症状变化，并继续追问最关键的缺失信息。";
            case "check_result" -> "重点围绕患者刚补充的检查、化验或影像信息给出下一步建议。";
            case "follow_up" -> "围绕恢复情况、复诊安排和风险观察点进行随访式沟通。";
            default -> "适合发送第一条接诊消息，先说明已接手，再给出下一步建议。";
        };
    }

    private String resolveFallbackSceneOpening(String sceneType,
                                               Doctor doctor,
                                               List<ConsultationMessageVO> messages) {
        boolean doctorHasReplied = hasDoctorMessage(messages, doctor == null ? null : doctor.getId());
        return switch (normalizeMessageDraftSceneType(sceneType, null, null)) {
            case "clarify" -> doctorHasReplied ? "已收到你刚刚补充的情况。" : "你好，我先根据你目前补充的情况继续帮你评估。";
            case "check_result" -> "已看到你补充的检查或化验信息。";
            case "follow_up" -> "这里继续跟进你目前的恢复情况。";
            default -> doctorHasReplied ? "已收到你刚刚补充的情况。" : "你好，我已经接手你的问诊。";
        };
    }

    private String resolveFallbackSceneAdvice(String sceneType) {
        return switch (normalizeMessageDraftSceneType(sceneType, null, null)) {
            case "clarify" -> "为了更准确判断，建议继续补充症状变化、持续时间以及是否已经自行用药。";
            case "check_result" -> "结合你目前补充的结果，建议继续观察症状变化，并按需要补充关键检查信息。";
            case "follow_up" -> "建议继续观察恢复情况，并按计划复诊或线上随访。";
            default -> "结合你目前的描述，建议先继续观察症状变化，并按时补充关键信息。";
        };
    }

    private String buildAnswerSummary(AdminConsultationRecordVO record) {
        if (record == null || record.getAnswers() == null || record.getAnswers().isEmpty()) return null;
        return record.getAnswers().stream()
                .filter(item -> StringUtils.hasText(item.getFieldValue()))
                .map(item -> safeText(item.getFieldLabel(), "字段") + "：" + abbreviate(renderAnswerValue(item.getFieldType(), item.getFieldValue()), 60))
                .limit(10)
                .collect(Collectors.joining("\n"));
    }

    private String buildConversationHistory(List<ConsultationMessageVO> messages) {
        if (messages == null || messages.isEmpty()) return null;
        List<ConsultationMessageVO> history = messages.stream()
                .filter(item -> item != null && ("user".equals(item.getSenderType()) || "doctor".equals(item.getSenderType())))
                .toList();
        if (history.isEmpty()) return null;
        return history.stream()
                .skip(Math.max(history.size() - 8, 0))
                .map(item -> "[" + messageRoleLabel(item.getSenderType()) + "] " + safeText(item.getContent(), "(图片/空消息)"))
                .collect(Collectors.joining("\n"));
    }

    private String buildDoctorHandleSummary(AdminConsultationRecordVO record) {
        if (record == null) return null;
        List<String> segments = new ArrayList<>();
        if (record.getDoctorHandle() != null) {
            addSegment(segments, "处理摘要", record.getDoctorHandle().getSummary());
            addSegment(segments, "处理建议", record.getDoctorHandle().getMedicalAdvice());
            addSegment(segments, "随访计划", record.getDoctorHandle().getFollowUpPlan());
        }
        if (record.getDoctorConclusion() != null) {
            addSegment(segments, "结论方向", record.getDoctorConclusion().getDiagnosisDirection());
            addSegment(segments, "患者提示", record.getDoctorConclusion().getPatientInstruction());
        }
        return segments.isEmpty() ? null : String.join("；", segments);
    }

    private String buildDoctorConclusionSummary(AdminConsultationRecordVO record) {
        if (record == null || record.getDoctorConclusion() == null) return null;
        var conclusion = record.getDoctorConclusion();
        List<String> segments = new ArrayList<>();
        addSegment(segments, "病情等级", renderConditionLevelLabel(conclusion.getConditionLevel()));
        addSegment(segments, "处理去向", renderDispositionLabel(conclusion.getDisposition()));
        addSegment(segments, "诊断方向", conclusion.getDiagnosisDirection());
        if (Objects.equals(conclusion.getNeedFollowUp(), 1)) {
            Integer days = normalizePositiveInt(conclusion.getFollowUpWithinDays(), 1, 365);
            segments.add(days == null ? "随访建议：需要继续随访" : "随访建议：" + days + " 天内随访");
        }
        addSegment(segments, "患者提示", conclusion.getPatientInstruction());
        return segments.isEmpty() ? null : String.join("；", segments);
    }

    private String buildFollowUpHistorySummary(AdminConsultationRecordVO record) {
        if (record == null || record.getDoctorFollowUps() == null || record.getDoctorFollowUps().isEmpty()) return null;
        return record.getDoctorFollowUps().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ConsultationDoctorFollowUpVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ConsultationDoctorFollowUpVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(item -> {
                    List<String> segments = new ArrayList<>();
                    addSegment(segments, "方式", renderFollowUpTypeLabel(item.getFollowUpType()));
                    addSegment(segments, "状态", renderPatientStatusLabel(item.getPatientStatus()));
                    addSegment(segments, "摘要", item.getSummary());
                    addSegment(segments, "建议", item.getAdvice());
                    addSegment(segments, "下一步", item.getNextStep());
                    if (Objects.equals(item.getNeedRevisit(), 1) && item.getNextFollowUpDate() != null) {
                        segments.add("下次随访：" + formatDateText(item.getNextFollowUpDate()));
                    }
                    return String.join("；", segments);
                })
                .collect(Collectors.joining("\n"));
    }

    private ConsultationDoctorFollowUpVO latestDoctorFollowUp(AdminConsultationRecordVO record) {
        if (record == null || record.getDoctorFollowUps() == null || record.getDoctorFollowUps().isEmpty()) return null;
        return record.getDoctorFollowUps().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ConsultationDoctorFollowUpVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ConsultationDoctorFollowUpVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private JSONObject latestAiStructuredPayload(AdminConsultationRecordVO record) {
        if (record == null || record.getTriageSession() == null || record.getTriageSession().getMessages() == null) return null;
        List<cn.gugufish.entity.vo.response.TriageMessageVO> messages = record.getTriageSession().getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            var message = messages.get(i);
            if (!"assistant".equalsIgnoreCase(trimToNull(message.getRoleType()))) continue;
            String structuredContent = trimToNull(message.getStructuredContent());
            if (structuredContent == null) continue;
            try {
                return JSON.parseObject(structuredContent);
            } catch (Exception ignored) {
                continue;
            }
        }
        return null;
    }

    private String latestAiReplyText(AdminConsultationRecordVO record) {
        if (record == null || record.getTriageSession() == null || record.getTriageSession().getMessages() == null) return null;
        List<cn.gugufish.entity.vo.response.TriageMessageVO> messages = record.getTriageSession().getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            var message = messages.get(i);
            if (!"assistant".equalsIgnoreCase(trimToNull(message.getRoleType()))) continue;
            String content = trimToNull(message.getContent());
            if (content != null) return content;
        }
        return null;
    }

    private List<String> extractRiskFlags(AdminConsultationRecordVO record) {
        JSONObject payload = latestAiStructuredPayload(record);
        if (payload != null) {
            List<String> riskFlags = normalizeJsonArray(payload.getJSONArray("riskFlags"), 4, 30);
            if (!riskFlags.isEmpty()) return riskFlags;
        }
        if (record == null || record.getTriageResult() == null || !StringUtils.hasText(record.getTriageResult().getRiskFlagsJson())) return List.of();
        try {
            return normalizeTextList(JSON.parseArray(record.getTriageResult().getRiskFlagsJson(), String.class), 4, 30);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> extractNextQuestions(JSONObject payload) {
        if (payload == null) return List.of();
        return normalizeJsonArray(payload.getJSONArray("nextQuestions"), 3, 60);
    }

    private ConsultationMessageVO latestPatientMessage(List<ConsultationMessageVO> messages) {
        if (messages == null || messages.isEmpty()) return null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConsultationMessageVO message = messages.get(i);
            if (message != null && "user".equals(message.getSenderType()) && StringUtils.hasText(message.getContent())) {
                return message;
            }
        }
        return null;
    }

    private boolean hasDoctorMessage(List<ConsultationMessageVO> messages, Integer doctorId) {
        if (messages == null || messages.isEmpty() || doctorId == null) return false;
        return messages.stream().anyMatch(item -> item != null
                && "doctor".equals(item.getSenderType())
                && Objects.equals(item.getSenderId(), doctorId));
    }

    private List<String> normalizeJsonArray(JSONArray array, int limit, int textLimit) {
        if (array == null || array.isEmpty()) return List.of();
        return normalizeTextList(array.stream()
                .map(item -> item == null ? null : String.valueOf(item))
                .toList(), limit, textLimit);
    }

    private List<String> normalizeTextList(List<String> source, int limit, int textLimit) {
        if (source == null || source.isEmpty()) return List.of();
        Set<String> values = new LinkedHashSet<>();
        for (String item : source) {
            String text = trimToNull(item);
            if (text == null) continue;
            values.add(abbreviate(text, textLimit));
            if (values.size() >= limit) break;
        }
        return List.copyOf(values);
    }

    private String renderAnswerValue(String fieldType, String fieldValue) {
        if (!StringUtils.hasText(fieldValue)) return "";
        if ("switch".equalsIgnoreCase(fieldType)) {
            return "1".equals(fieldValue) ? "是" : "否";
        }
        if ("multi_select".equalsIgnoreCase(fieldType)) {
            try {
                return joinList(JSON.parseArray(fieldValue, String.class), "、");
            } catch (Exception ignored) {
                return fieldValue;
            }
        }
        if ("upload".equalsIgnoreCase(fieldType)) {
            try {
                List<String> paths = JSON.parseArray(fieldValue, String.class);
                return paths == null || paths.isEmpty() ? "已上传资料" : "已上传 " + paths.size() + " 张图片";
            } catch (Exception ignored) {
                return "已上传资料";
            }
        }
        return fieldValue;
    }

    private void addSegment(List<String> segments, String label, String value) {
        String text = trimToNull(value);
        if (text == null) return;
        segments.add(label + "：" + abbreviate(text, 80));
    }

    private String joinSegments(List<String> segments) {
        return segments.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining());
    }

    private String joinList(List<String> values, String separator) {
        List<String> normalized = values == null ? List.of() : values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();
        return normalized.isEmpty() ? null : String.join(separator, normalized);
    }

    private String messageRoleLabel(String senderType) {
        return switch (trimToNull(senderType) == null ? "" : trimToNull(senderType)) {
            case "user" -> "患者";
            case "doctor" -> "医生";
            default -> "沟通";
        };
    }

    private String renderVisitType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        return switch (normalized.toLowerCase()) {
            case "emergency" -> "立即急诊";
            case "offline" -> "尽快线下就医";
            case "followup" -> "复诊随访";
            case "online" -> "线上继续沟通";
            default -> normalized;
        };
    }

    private String renderConditionLevelLabel(String value) {
        String normalized = normalizeConditionLevel(value);
        if (normalized == null) return null;
        return switch (normalized) {
            case "low" -> "轻度";
            case "medium" -> "中度";
            case "high" -> "较高风险";
            case "critical" -> "危急";
            default -> normalized;
        };
    }

    private String renderDispositionLabel(String value) {
        String normalized = normalizeDisposition(value);
        if (normalized == null) return null;
        return switch (normalized) {
            case "observe" -> "继续观察";
            case "online_followup" -> "线上随访";
            case "offline_visit" -> "线下就医";
            case "emergency" -> "立即急诊";
            default -> normalized;
        };
    }

    private String renderFollowUpTypeLabel(String value) {
        return switch (normalizeFollowUpType(value)) {
            case "phone" -> "电话随访";
            case "offline" -> "线下随访";
            case "other" -> "其他方式";
            default -> "平台随访";
        };
    }

    private String renderPatientStatusLabel(String value) {
        return switch (normalizePatientStatus(value)) {
            case "improved" -> "明显好转";
            case "worsened" -> "出现加重";
            case "other" -> "其他情况";
            default -> "基本稳定";
        };
    }

    private String safeText(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }

    private String safeNumber(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String firstText(String first, String second) {
        String left = trimToNull(first);
        return left == null ? trimToNull(second) : left;
    }

    private String abbreviate(String value, int maxLength) {
        String text = trimToNull(value);
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, Math.max(maxLength - 3, 0)) + "...";
    }

    private String normalizeConditionLevel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("low", "medium", "high", "critical").contains(normalized) ? normalized : null;
    }

    private String normalizeDisposition(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase();
        return List.of("observe", "online_followup", "offline_visit", "emergency").contains(normalized) ? normalized : null;
    }

    private String normalizeFollowUpType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return "platform";
        normalized = normalized.toLowerCase();
        return List.of("platform", "phone", "offline", "other").contains(normalized) ? normalized : "platform";
    }

    private String normalizePatientStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return "stable";
        normalized = normalized.toLowerCase();
        return List.of("improved", "stable", "worsened", "other").contains(normalized) ? normalized : "stable";
    }

    private Integer normalizeBinaryFlag(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    private Integer normalizePositiveInt(Integer value, int min, int max) {
        if (value == null) return null;
        if (value < min || value > max) return null;
        return value;
    }

    private String resolveConditionLevelCode(AdminConsultationRecordVO record) {
        if (record == null) return "medium";
        if (record.getDoctorConclusion() != null) {
            String saved = normalizeConditionLevel(record.getDoctorConclusion().getConditionLevel());
            if (saved != null) return saved;
        }
        String text = firstText(
                trimToNull(record.getTriageResult() == null ? null : record.getTriageResult().getTriageLevelCode()),
                firstText(trimToNull(record.getTriageLevelCode()), trimToNull(record.getTriageLevelName()))
        );
        String normalized = text == null ? "" : text.toUpperCase();
        if (normalized.contains("EMERGENCY") || normalized.contains("CRITICAL") || normalized.contains("RED")) return "critical";
        if (normalized.contains("OFFLINE") || normalized.contains("HIGH") || normalized.contains("ORANGE")) return "high";
        if (normalized.contains("FOLLOWUP") || normalized.contains("MEDIUM") || normalized.contains("YELLOW")) return "medium";
        if (normalized.contains("ONLINE") || normalized.contains("LOW") || normalized.contains("GREEN")) return "low";
        return "medium";
    }

    private String resolveDispositionCode(AdminConsultationRecordVO record) {
        if (record == null) return "observe";
        if (record.getDoctorConclusion() != null) {
            String saved = normalizeDisposition(record.getDoctorConclusion().getDisposition());
            if (saved != null) return saved;
        }
        String visitType = trimToNull(record.getTriageActionType());
        if (visitType == null) return "observe";
        return switch (visitType.toLowerCase()) {
            case "emergency" -> "emergency";
            case "offline" -> "offline_visit";
            case "followup" -> "online_followup";
            case "online" -> "observe";
            default -> "observe";
        };
    }

    private boolean isHighRiskDisposition(String disposition) {
        return "offline_visit".equals(normalizeDisposition(disposition))
                || "emergency".equals(normalizeDisposition(disposition));
    }

    private Integer resolveNeedFollowUp(AdminConsultationRecordVO record, String disposition) {
        if (record != null && record.getDoctorConclusion() != null && record.getDoctorConclusion().getNeedFollowUp() != null) {
            return normalizeBinaryFlag(record.getDoctorConclusion().getNeedFollowUp());
        }
        return "online_followup".equals(normalizeDisposition(disposition))
                || "followup".equalsIgnoreCase(trimToNull(record == null ? null : record.getTriageActionType()))
                ? 1
                : 0;
    }

    private Integer resolveFollowUpWithinDays(AdminConsultationRecordVO record, int defaultDays) {
        if (record != null && record.getDoctorConclusion() != null) {
            Integer days = normalizePositiveInt(record.getDoctorConclusion().getFollowUpWithinDays(), 1, 365);
            if (days != null) return days;
        }
        return defaultDays;
    }

    private List<String> buildConclusionTagSuggestions(String disposition, Integer needFollowUp, List<String> riskFlags) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if ("observe".equals(normalizeDisposition(disposition))) tags.add("适合居家观察");
        if (isHighRiskDisposition(disposition)) tags.add("建议线下检查");
        if (Objects.equals(needFollowUp, 1)) tags.add("需要复诊随访");
        String riskText = joinList(riskFlags, "、");
        if (riskText != null && riskText.contains("过敏")) tags.add("过敏风险");
        if (riskText != null && riskText.contains("发热")) tags.add("发热监测");
        if (riskText != null && (riskText.contains("皮肤") || riskText.contains("皮疹"))) tags.add("皮肤护理");
        if (riskText != null && (riskText.contains("血压") || riskText.contains("血糖") || riskText.contains("慢病"))) tags.add("慢病管理");
        return tags.stream().limit(4).toList();
    }

    private String buildPatientInstructionFromRecord(AdminConsultationRecordVO record,
                                                     List<String> riskFlags,
                                                     boolean highRisk) {
        List<String> segments = new ArrayList<>();
        addSegment(segments, "既往提示", record == null || record.getDoctorConclusion() == null ? null : record.getDoctorConclusion().getPatientInstruction());
        if (!riskFlags.isEmpty()) {
            segments.add("如出现" + joinList(riskFlags.stream().limit(3).toList(), "、") + "等情况，请及时线下就医。");
        } else if (highRisk) {
            segments.add("如症状持续加重、精神反应差或出现新的高风险表现，请立即线下就医或急诊。");
        } else if (resolveNeedFollowUp(record, resolveDispositionCode(record)) == 1) {
            Integer days = resolveFollowUpWithinDays(record, 3);
            segments.add("请继续观察症状变化，并按计划在 " + days + " 天内复诊或线上随访。");
        } else {
            segments.add("如症状持续不缓解或再次加重，请及时复诊。");
        }
        return abbreviate(String.join("；", segments), 500);
    }

    private String resolvePatientStatus(ConsultationMessageVO latestPatientMessage,
                                        ConsultationDoctorFollowUpVO latestFollowUp) {
        String content = trimToNull(latestPatientMessage == null ? null : latestPatientMessage.getContent());
        if (content != null) {
            if (containsAny(content, "加重", "恶化", "更严重", "反复高热", "呼吸困难", "胸闷", "疼得更厉害")) return "worsened";
            if (containsAny(content, "好转", "缓解", "减轻", "退烧", "恢复", "舒服一些")) return "improved";
            return "stable";
        }
        if (latestFollowUp != null) {
            return normalizePatientStatus(latestFollowUp.getPatientStatus());
        }
        return "stable";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) return false;
        for (String keyword : keywords) {
            if (keyword != null && text.contains(keyword)) return true;
        }
        return false;
    }

    private Integer resolveNeedRevisit(AdminConsultationRecordVO record,
                                       String patientStatus,
                                       boolean highRisk) {
        if (highRisk || "worsened".equals(normalizePatientStatus(patientStatus))) return 1;
        ConsultationDoctorFollowUpVO latest = latestDoctorFollowUp(record);
        if (latest != null && Objects.equals(latest.getNeedRevisit(), 1)) return 1;
        if (record != null && record.getDoctorConclusion() != null && Objects.equals(record.getDoctorConclusion().getNeedFollowUp(), 1)) return 1;
        return 0;
    }

    private String resolveNextFollowUpDateText(AdminConsultationRecordVO record,
                                               ConsultationDoctorFollowUpVO latestFollowUp,
                                               String patientStatus) {
        if (latestFollowUp != null && Objects.equals(latestFollowUp.getNeedRevisit(), 1) && latestFollowUp.getNextFollowUpDate() != null) {
            LocalDate date = latestFollowUp.getNextFollowUpDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!date.isBefore(LocalDate.now())) {
                return date.toString();
            }
        }
        int days = "worsened".equals(normalizePatientStatus(patientStatus))
                ? 1
                : resolveFollowUpWithinDays(record, 3);
        return LocalDate.now().plusDays(Math.max(days, 1)).toString();
    }

    private String resolveFollowUpType(ConsultationDoctorFollowUpVO latestFollowUp) {
        return latestFollowUp == null ? "platform" : normalizeFollowUpType(latestFollowUp.getFollowUpType());
    }

    private String normalizeNextFollowUpDateText(String value, int fallbackDays) {
        String text = trimToNull(value);
        if (text != null) {
            try {
                LocalDate date = LocalDate.parse(text);
                if (!date.isBefore(LocalDate.now())) {
                    return date.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now().plusDays(Math.max(fallbackDays, 1)).toString();
    }

    private String formatDateText(Date value) {
        if (value == null) return null;
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
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

    private ConsultationServiceFeedback findServiceFeedback(int consultationId) {
        return consultationServiceFeedbackMapper.selectOne(Wrappers.<ConsultationServiceFeedback>query()
                .eq("consultation_id", consultationId)
                .eq("status", 1)
                .orderByDesc("id")
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

    private List<AdminConsultationRecordVO> buildRecommendedConsultations(List<AdminConsultationRecordVO> consultations, Integer doctorId) {
        return consultations.stream()
                .filter(item -> isRecommendedToDoctor(item, doctorId))
                .sorted(Comparator
                        .comparing(this::isRiskConsultation).reversed()
                        .thenComparing(this::messageSortTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminConsultationRecordVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AdminConsultationRecordVO> buildUnclaimedConsultations(List<AdminConsultationRecordVO> consultations) {
        return consultations.stream()
                .filter(this::isUnclaimedConsultation)
                .sorted(Comparator
                        .comparing(this::isRiskConsultation).reversed()
                        .thenComparing(this::messageSortTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminConsultationRecordVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AdminConsultationRecordVO> buildUnreadConsultations(List<AdminConsultationRecordVO> consultations, Integer doctorId) {
        return consultations.stream()
                .filter(item -> isActionableForDoctor(item, doctorId))
                .filter(item -> item.getMessageSummary() != null && defaultInt(item.getMessageSummary().getUnreadCount()) > 0)
                .sorted(Comparator
                        .comparing(this::messageSortTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminConsultationRecordVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AdminConsultationRecordVO> buildWaitingReplyConsultations(List<AdminConsultationRecordVO> consultations, Integer doctorId) {
        return consultations.stream()
                .filter(item -> isActionableForDoctor(item, doctorId))
                .filter(this::isWaitingReplyConsultation)
                .sorted(Comparator
                        .comparing(this::messageSortTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AdminConsultationRecordVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AdminConsultationRecordVO> buildPendingFollowUpConsultations(List<AdminConsultationRecordVO> consultations, Integer doctorId) {
        List<Integer> consultationIds = consultations.stream()
                .map(AdminConsultationRecordVO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (consultationIds.isEmpty()) return List.of();

        Map<Integer, ConsultationDoctorHandle> handleMap = new HashMap<>();
        consultationDoctorHandleMapper.selectList(Wrappers.<ConsultationDoctorHandle>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> handleMap.putIfAbsent(item.getConsultationId(), item));

        Map<Integer, ConsultationDoctorConclusion> conclusionMap = new HashMap<>();
        consultationDoctorConclusionMapper.selectList(Wrappers.<ConsultationDoctorConclusion>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> conclusionMap.putIfAbsent(item.getConsultationId(), item));

        Map<Integer, ConsultationDoctorFollowUp> latestFollowUpMap = new HashMap<>();
        consultationDoctorFollowUpMapper.selectList(Wrappers.<ConsultationDoctorFollowUp>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .forEach(item -> latestFollowUpMap.putIfAbsent(item.getConsultationId(), item));

        return consultations.stream()
                .filter(item -> "completed".equals(item.getStatus()))
                .filter(item -> {
                    ConsultationDoctorHandle handle = handleMap.get(item.getId());
                    ConsultationDoctorConclusion conclusion = conclusionMap.get(item.getId());
                    ConsultationDoctorFollowUp latestFollowUp = latestFollowUpMap.get(item.getId());
                    return isPendingFollowUp(item, doctorId, handle, conclusion, latestFollowUp);
                })
                .map(item -> {
                    ConsultationDoctorHandle handle = handleMap.get(item.getId());
                    ConsultationDoctorConclusion conclusion = conclusionMap.get(item.getId());
                    ConsultationDoctorFollowUp latestFollowUp = latestFollowUpMap.get(item.getId());
                    AdminConsultationRecordVO target = copyRecord(item);
                    if (handle != null) {
                        target.setDoctorHandle(handle.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO.class));
                    }
                    if (conclusion != null) {
                        target.setDoctorConclusion(conclusion.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO.class));
                    }
                    target.setDoctorFollowUps(latestFollowUp == null
                            ? List.of()
                            : List.of(latestFollowUp.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorFollowUpVO.class)));
                    return target;
                })
                .sorted(Comparator
                        .comparing(this::followUpDueTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AdminConsultationRecordVO::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean isActionableForDoctor(AdminConsultationRecordVO record, Integer doctorId) {
        if (record == null || doctorId == null) return false;
        return isUnclaimedConsultation(record) || isClaimedByDoctor(record, doctorId);
    }

    private boolean isRecommendedToDoctor(AdminConsultationRecordVO record, Integer doctorId) {
        if (record == null || doctorId == null || record.getSmartDispatch() == null) return false;
        return Objects.equals(record.getSmartDispatch().getSuggestedDoctorId(), doctorId)
                && "waiting_accept".equals(record.getSmartDispatch().getStatus());
    }

    private boolean isUnclaimedConsultation(AdminConsultationRecordVO record) {
        if (record == null) return false;
        var assignment = record.getDoctorAssignment();
        return assignment == null || !"claimed".equals(assignment.getStatus());
    }

    private boolean isClaimedByDoctor(AdminConsultationRecordVO record, Integer doctorId) {
        if (record == null || doctorId == null) return false;
        var assignment = record.getDoctorAssignment();
        return assignment != null
                && "claimed".equals(assignment.getStatus())
                && Objects.equals(assignment.getDoctorId(), doctorId);
    }

    private boolean isWaitingReplyConsultation(AdminConsultationRecordVO record) {
        if (record == null || record.getMessageSummary() == null) return false;
        return defaultInt(record.getMessageSummary().getTotalCount()) > 0
                && "user".equals(record.getMessageSummary().getLatestSenderType());
    }

    private boolean isPendingFollowUp(AdminConsultationRecordVO record,
                                      Integer doctorId,
                                      ConsultationDoctorHandle handle,
                                      ConsultationDoctorConclusion conclusion,
                                      ConsultationDoctorFollowUp latestFollowUp) {
        if (record == null || doctorId == null) return false;
        if (handle == null || !Objects.equals(handle.getDoctorId(), doctorId)) return false;
        if (conclusion == null || !Objects.equals(conclusion.getNeedFollowUp(), 1)) return false;
        return latestFollowUp == null || Objects.equals(latestFollowUp.getNeedRevisit(), 1);
    }

    private Date messageSortTime(AdminConsultationRecordVO record) {
        if (record == null || record.getMessageSummary() == null) return record == null ? null : record.getUpdateTime();
        return record.getMessageSummary().getLatestTime() == null ? record.getUpdateTime() : record.getMessageSummary().getLatestTime();
    }

    private Date followUpDueTime(AdminConsultationRecordVO record) {
        if (record == null) return null;
        if (record.getDoctorFollowUps() != null && !record.getDoctorFollowUps().isEmpty()) {
            Date nextFollowUpDate = record.getDoctorFollowUps().get(0).getNextFollowUpDate();
            if (nextFollowUpDate != null) return nextFollowUpDate;
        }
        if (record.getDoctorConclusion() != null && record.getDoctorConclusion().getFollowUpWithinDays() != null) {
            Date baseTime = record.getDoctorConclusion().getUpdateTime();
            if (baseTime == null) {
                baseTime = record.getDoctorHandle() == null ? record.getUpdateTime() : record.getDoctorHandle().getCompleteTime();
            }
            if (baseTime != null) {
                return Date.from(baseTime.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .plus(record.getDoctorConclusion().getFollowUpWithinDays(), ChronoUnit.DAYS)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
            }
        }
        return record.getUpdateTime();
    }

    private boolean isDueTodayFollowUp(AdminConsultationRecordVO record) {
        Date dueTime = followUpDueTime(record);
        if (dueTime == null) return false;
        return dueTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .isEqual(LocalDate.now());
    }

    private boolean isOverdueFollowUp(AdminConsultationRecordVO record) {
        Date dueTime = followUpDueTime(record);
        if (dueTime == null) return false;
        return dueTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .isBefore(LocalDate.now());
    }

    private AdminConsultationRecordVO copyRecord(AdminConsultationRecordVO source) {
        AdminConsultationRecordVO target = new AdminConsultationRecordVO();
        BeanUtils.copyProperties(source, target);
        return target;
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

    private Double averageServiceScore(List<ConsultationServiceFeedbackVO> serviceFeedbacks) {
        double average = serviceFeedbacks.stream()
                .map(ConsultationServiceFeedbackVO::getServiceScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);
        return Double.isNaN(average) ? null : roundToOneDecimal(average);
    }

    private boolean isAttentionServiceFeedback(ConsultationServiceFeedbackVO feedback) {
        if (feedback == null) return false;
        if (Objects.equals(feedback.getDoctorHandleStatus(), 1)) return false;
        if (!Objects.equals(feedback.getIsResolved(), 1)) return true;
        return feedback.getServiceScore() != null && feedback.getServiceScore() <= 2;
    }

    private Double roundToOneDecimal(double value) {
        return Math.round(value * 10D) / 10D;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
