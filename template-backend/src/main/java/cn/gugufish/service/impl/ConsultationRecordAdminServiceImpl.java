package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationCheckSuggestion;
import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.dto.ConsultationMedicationFeedback;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.ConsultationReportFeedback;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.response.AdminConsultationAiDoctorVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiFieldVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiFieldSampleVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiGroupVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiMismatchVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiReasonVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiSummaryVO;
import cn.gugufish.entity.vo.response.AdminConsultationDispatchDoctorVO;
import cn.gugufish.entity.vo.response.AdminConsultationDispatchSummaryVO;
import cn.gugufish.entity.vo.response.AdminConsultationDispatchWaitVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationAiComparisonVO;
import cn.gugufish.entity.vo.response.ConsultationCheckSuggestionVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;
import cn.gugufish.entity.vo.response.ConsultationMedicationFeedbackVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.ConsultationReportFeedbackVO;
import cn.gugufish.entity.vo.response.ConsultationSmartDispatchVO;
import cn.gugufish.entity.vo.response.TriageMessageVO;
import cn.gugufish.entity.vo.response.TriageRuleHitLogVO;
import cn.gugufish.entity.vo.response.TriageResultVO;
import cn.gugufish.entity.vo.response.TriageSessionVO;
import cn.gugufish.mapper.ConsultationCheckSuggestionMapper;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.mapper.ConsultationMedicationFeedbackMapper;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.ConsultationReportFeedbackMapper;
import cn.gugufish.mapper.TriageMessageMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.ConsultationDoctorAssignmentQueryService;
import cn.gugufish.service.ConsultationDispatchConfigService;
import cn.gugufish.service.ConsultationPaymentService;
import cn.gugufish.service.ConsultationPrescriptionService;
import cn.gugufish.service.ConsultationRecordAdminService;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import cn.gugufish.service.ConsultationServiceFeedbackQueryService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import cn.gugufish.utils.ConsultationAiComparisonUtils;
import cn.gugufish.utils.ConsultationAiMismatchReasonUtils;
import cn.gugufish.utils.ConsultationSmartDispatchUtils;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class ConsultationRecordAdminServiceImpl implements ConsultationRecordAdminService {

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationRecordAnswerMapper consultationRecordAnswerMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Resource
    ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper;

    @Resource
    ConsultationCheckSuggestionMapper consultationCheckSuggestionMapper;

    @Resource
    ConsultationReportFeedbackMapper consultationReportFeedbackMapper;

    @Resource
    ConsultationMedicationFeedbackMapper consultationMedicationFeedbackMapper;

    @Resource
    TriageRuleHitLogMapper triageRuleHitLogMapper;

    @Resource
    TriageSessionMapper triageSessionMapper;

    @Resource
    TriageMessageMapper triageMessageMapper;

    @Resource
    TriageResultMapper triageResultMapper;

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
    ConsultationDispatchConfigService consultationDispatchConfigService;

    @Resource
    ConsultationPrescriptionService consultationPrescriptionService;

    @Resource
    ConsultationPaymentService consultationPaymentService;

    @Override
    public List<AdminConsultationRecordVO> listRecords() {
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) return List.of();

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, cn.gugufish.entity.vo.response.ConsultationPaymentVO> paymentMap = consultationPaymentService.mapByConsultationIds(consultationIds);
        Map<Integer, ConsultationDoctorAssignment> assignmentMap = loadLatestAssignmentMap(consultationIds);
        Map<Integer, TriageResult> triageResultMap = loadLatestTriageResultEntityMap(consultationIds);
        Map<Integer, cn.gugufish.entity.vo.response.ConsultationServiceFeedbackVO> serviceFeedbackMap =
                consultationServiceFeedbackQueryService.mapByConsultationIds(consultationIds);

        return records.stream()
                .map(item -> item.asViewObject(AdminConsultationRecordVO.class, vo -> {
                    ConsultationDoctorAssignment assignment = assignmentMap.get(item.getId());
                    TriageResult triageResult = triageResultMap.get(item.getId());
                    if (assignment != null) {
                        vo.setDoctorAssignment(assignment.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO.class));
                    }
                    vo.setPayment(paymentMap.get(item.getId()));
                    vo.setSmartDispatch(ConsultationSmartDispatchUtils.build(
                            assignment == null ? null : assignment.getDoctorId(),
                            assignment == null ? null : assignment.getDoctorName(),
                            assignment == null ? null : assignment.getStatus(),
                            triageResult == null ? null : triageResult.getDoctorId(),
                            triageResult == null ? null : triageResult.getDoctorName(),
                            triageResult == null ? null : triageResult.getDoctorCandidatesJson(),
                            triageResult == null ? null : triageResult.getReasonText()
                    ));
                    vo.setServiceFeedback(serviceFeedbackMap.get(item.getId()));
                }))
                .toList();
    }

    @Override
    public AdminConsultationDispatchSummaryVO smartDispatchSummary() {
        ConsultationDispatchConfig dispatchConfig = consultationDispatchConfigService.getConfig();
        int overdueHours = resolveWaitingOverdueHours(dispatchConfig);
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) {
            return emptyDispatchSummary(overdueHours);
        }

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, ConsultationDoctorAssignment> assignmentMap = loadLatestAssignmentMap(consultationIds);
        Map<Integer, TriageResult> triageResultMap = loadLatestTriageResultEntityMap(consultationIds);

        int totalCount = records.size();
        int suggestedCount = 0;
        int waitingAcceptCount = 0;
        int claimedBySuggestedCount = 0;
        int claimedByOtherCount = 0;
        int departmentQueueCount = 0;
        int overdueWaitingCount = 0;
        long totalClaimMinutes = 0L;
        int claimSampleCount = 0;

        Map<String, DispatchDoctorAccumulator> doctorMap = new HashMap<>();
        List<AdminConsultationDispatchWaitVO> waitingRecords = new ArrayList<>();

        for (ConsultationRecord record : records) {
            ConsultationDoctorAssignment assignment = assignmentMap.get(record.getId());
            TriageResult triageResult = triageResultMap.get(record.getId());
            ConsultationSmartDispatchVO dispatch = ConsultationSmartDispatchUtils.build(
                    assignment == null ? null : assignment.getDoctorId(),
                    assignment == null ? null : assignment.getDoctorName(),
                    assignment == null ? null : assignment.getStatus(),
                    triageResult == null ? null : triageResult.getDoctorId(),
                    triageResult == null ? null : triageResult.getDoctorName(),
                    triageResult == null ? null : triageResult.getDoctorCandidatesJson(),
                    triageResult == null ? null : triageResult.getReasonText()
            );

            String dispatchStatus = trimToNull(dispatch.getStatus());
            boolean hasSuggestedDoctor = dispatch.getSuggestedDoctorId() != null || trimToNull(dispatch.getSuggestedDoctorName()) != null;
            if (hasSuggestedDoctor) {
                suggestedCount++;
                DispatchDoctorAccumulator accumulator = doctorMap.computeIfAbsent(resolveDispatchDoctorKey(dispatch), key -> new DispatchDoctorAccumulator());
                if (accumulator.doctorId == null && dispatch.getSuggestedDoctorId() != null) {
                    accumulator.doctorId = dispatch.getSuggestedDoctorId();
                }
                if (accumulator.doctorName == null) {
                    accumulator.doctorName = normalizeGroupName(dispatch.getSuggestedDoctorName(), "未标记医生");
                }
                if (accumulator.departmentName == null) {
                    accumulator.departmentName = normalizeGroupName(record.getDepartmentName(), "未分配科室");
                }
                accumulator.totalSuggestedCount++;

                if (Objects.equals(dispatchStatus, "waiting_accept")) {
                    accumulator.waitingAcceptCount++;
                } else if (Objects.equals(dispatchStatus, "claimed_by_suggested")) {
                    accumulator.acceptedCount++;
                    Long claimMinutes = resolveClaimDurationMinutes(record, assignment);
                    if (claimMinutes != null) {
                        accumulator.acceptedClaimMinutes += claimMinutes;
                        accumulator.acceptedClaimSampleCount++;
                    }
                } else if (Objects.equals(dispatchStatus, "claimed_by_other")) {
                    accumulator.lostCount++;
                }
            }

            if (Objects.equals(dispatchStatus, "waiting_accept")) {
                waitingAcceptCount++;
                if (isWaitingOverHours(record.getCreateTime(), overdueHours)) {
                    overdueWaitingCount++;
                }
                waitingRecords.add(buildDispatchWaitItem(record, dispatch));
            } else if (Objects.equals(dispatchStatus, "claimed_by_suggested")) {
                claimedBySuggestedCount++;
                Long claimMinutes = resolveClaimDurationMinutes(record, assignment);
                if (claimMinutes != null) {
                    totalClaimMinutes += claimMinutes;
                    claimSampleCount++;
                }
            } else if (Objects.equals(dispatchStatus, "claimed_by_other")) {
                claimedByOtherCount++;
                Long claimMinutes = resolveClaimDurationMinutes(record, assignment);
                if (claimMinutes != null) {
                    totalClaimMinutes += claimMinutes;
                    claimSampleCount++;
                }
            } else if (Objects.equals(dispatchStatus, "department_queue")) {
                departmentQueueCount++;
            }
        }

        int claimedRecommendedCount = claimedBySuggestedCount + claimedByOtherCount;
        AdminConsultationDispatchSummaryVO summary = new AdminConsultationDispatchSummaryVO();
        summary.setTotalRecordCount(totalCount);
        summary.setSuggestedCount(suggestedCount);
        summary.setWaitingAcceptCount(waitingAcceptCount);
        summary.setClaimedBySuggestedCount(claimedBySuggestedCount);
        summary.setClaimedByOtherCount(claimedByOtherCount);
        summary.setDepartmentQueueCount(departmentQueueCount);
        summary.setOverdueWaitingCount(overdueWaitingCount);
        summary.setOverdueThresholdHours(overdueHours);
        summary.setSuggestionCoverageText(suggestedCount + " / " + totalCount);
        summary.setSuggestedHitRateText(formatPercent(claimedBySuggestedCount, claimedRecommendedCount));
        summary.setClaimedByOtherRateText(formatPercent(claimedByOtherCount, claimedRecommendedCount));
        summary.setAverageClaimDurationText(formatAverageDuration(totalClaimMinutes, claimSampleCount));
        summary.setDoctorBreakdown(buildDispatchDoctorBreakdown(doctorMap));
        summary.setRecentWaitingRecords(waitingRecords.stream()
                .sorted(Comparator
                        .comparing(AdminConsultationDispatchWaitVO::getCreateTime, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(AdminConsultationDispatchWaitVO::getConsultationId, Comparator.nullsLast(Integer::compareTo)))
                .limit(6)
                .toList());
        return summary;
    }

    @Override
    public AdminConsultationAiSummaryVO aiSummary() {
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) {
            AdminConsultationAiSummaryVO summary = new AdminConsultationAiSummaryVO();
            summary.setTotalRecordCount(0);
            summary.setComparedCount(0);
            summary.setConsistentCount(0);
            summary.setMismatchCount(0);
            summary.setPendingCount(0);
            summary.setCoverageText("0 / 0");
            summary.setConsistentRateText("-");
            summary.setDepartmentBreakdown(List.of());
            summary.setCategoryBreakdown(List.of());
            summary.setFieldBreakdown(List.of());
            summary.setDoctorBreakdown(List.of());
            summary.setMismatchReasonBreakdown(List.of());
            summary.setRecentMismatchRecords(List.of());
            return summary;
        }

        Map<Integer, ConsultationRecord> recordMap = new HashMap<>();
        records.forEach(item -> recordMap.put(item.getId(), item));

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, ConsultationDoctorConclusion> latestConclusionMap = loadLatestConclusionMap(consultationIds);
        Map<Integer, ConsultationAiComparisonVO> aiComparisonMap = buildAiComparisonMap(records, latestConclusionMap);

        int totalCount = records.size();
        int comparedCount = 0;
        int consistentCount = 0;
        int mismatchCount = 0;
        for (ConsultationRecord record : records) {
            ConsultationDoctorConclusion conclusion = latestConclusionMap.get(record.getId());
            if (conclusion == null || conclusion.getIsConsistentWithAi() == null) continue;
            comparedCount++;
            if (conclusion.getIsConsistentWithAi() == 1) consistentCount++;
            if (conclusion.getIsConsistentWithAi() == 0) mismatchCount++;
        }

        List<AdminConsultationAiMismatchVO> mismatchRecords = latestConclusionMap.values().stream()
                .filter(item -> item.getIsConsistentWithAi() != null && item.getIsConsistentWithAi() == 0)
                .sorted((a, b) -> {
                    if (a.getUpdateTime() == null && b.getUpdateTime() == null) return Integer.compare(b.getId(), a.getId());
                    if (a.getUpdateTime() == null) return 1;
                    if (b.getUpdateTime() == null) return -1;
                    return b.getUpdateTime().compareTo(a.getUpdateTime());
                })
                .limit(6)
                .map(item -> buildMismatchItem(recordMap.get(item.getConsultationId()), item, aiComparisonMap.get(item.getConsultationId())))
                .filter(Objects::nonNull)
                .toList();

        AdminConsultationAiSummaryVO summary = new AdminConsultationAiSummaryVO();
        summary.setTotalRecordCount(totalCount);
        summary.setComparedCount(comparedCount);
        summary.setConsistentCount(consistentCount);
        summary.setMismatchCount(mismatchCount);
        summary.setPendingCount(Math.max(totalCount - comparedCount, 0));
        summary.setCoverageText(comparedCount + " / " + totalCount);
        summary.setConsistentRateText(comparedCount <= 0 ? "-" : formatPercent(consistentCount, comparedCount));
        summary.setDepartmentBreakdown(buildBreakdown(records, latestConclusionMap, ConsultationRecord::getDepartmentName, "未分配科室"));
        summary.setCategoryBreakdown(buildBreakdown(records, latestConclusionMap, ConsultationRecord::getCategoryName, "未分类"));
        summary.setFieldBreakdown(buildFieldBreakdown(records, latestConclusionMap, aiComparisonMap));
        List<ConsultationDoctorConclusion> latestConclusions = latestConclusionMap.values().stream().toList();
        summary.setDoctorBreakdown(buildDoctorBreakdown(latestConclusions));
        summary.setMismatchReasonBreakdown(buildMismatchReasonBreakdown(latestConclusions));
        summary.setRecentMismatchRecords(mismatchRecords);
        return summary;
    }

    @Override
    public AdminConsultationRecordVO recordDetail(int id) {
        ConsultationRecord record = consultationRecordMapper.selectById(id);
        if (record == null) return null;

        List<ConsultationRecordAnswerVO> answers = consultationRecordAnswerMapper.selectList(Wrappers.<ConsultationRecordAnswer>query()
                        .eq("consultation_id", id)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationRecordAnswerVO.class))
                .toList();

        List<TriageRuleHitLogVO> ruleHits = triageRuleHitLogMapper.selectList(Wrappers.<TriageRuleHitLog>query()
                        .eq("consultation_id", id)
                        .orderByDesc("is_primary")
                        .orderByDesc("priority")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(TriageRuleHitLogVO.class))
                .toList();
        var doctorAssignment = consultationDoctorAssignmentQueryService.detailByConsultationId(id);
        var doctorHandle = consultationDoctorHandleQueryService.detailByConsultationId(id);
        var doctorConclusion = consultationDoctorConclusionQueryService.detailByConsultationId(id);
        var payment = consultationPaymentService.detailByConsultationId(id);
        var checkSuggestions = listCheckSuggestions(id);
        var reportFeedbacks = listReportFeedbacks(id);
        var medicationFeedbacks = listMedicationFeedbacks(id);
        var prescriptions = consultationPrescriptionService.listByConsultationId(id);
        var doctorFollowUps = consultationDoctorFollowUpQueryService.listByConsultationId(id);
        var triageSession = triageSessionQueryService.detailByConsultationId(id);
        var triageResult = triageResultQueryService.detailByConsultationId(id);
        var triageFeedback = triageFeedbackQueryService.detailByConsultationId(id);
        var serviceFeedback = consultationServiceFeedbackQueryService.detailByConsultationId(id);

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
            vo.setAiComparison(ConsultationAiComparisonUtils.build(doctorConclusion, triageSession, triageResult));
            vo.setDoctorFollowUps(doctorFollowUps);
            vo.setTriageSession(triageSession);
            vo.setTriageResult(triageResult);
            vo.setTriageFeedback(triageFeedback);
            vo.setServiceFeedback(serviceFeedback);
        });
    }

    @Override
    public List<AdminConsultationAiMismatchVO> mismatchSamples(int limit,
                                                               int offset,
                                                               String keyword,
                                                               String doctorName,
                                                               String reasonCode,
                                                               String categoryName,
                                                               String departmentName) {
        return paginate(
                collectMismatchSamples(
                        keyword,
                        doctorName,
                        reasonCode,
                        categoryName,
                        departmentName
                ),
                limit,
                offset
        );
    }

    @Override
    public List<AdminConsultationAiFieldSampleVO> fieldSamples(String fieldKey,
                                                               String status,
                                                               int limit,
                                                               int offset,
                                                               String keyword,
                                                               String doctorName,
                                                               String categoryName,
                                                               String departmentName) {
        return paginate(
                collectFieldSamples(
                        fieldKey,
                        status,
                        keyword,
                        doctorName,
                        categoryName,
                        departmentName
                ),
                limit,
                offset
        );
    }

    @Override
    public byte[] exportMismatchSamplesCsv(String keyword,
                                           String doctorName,
                                           String reasonCode,
                                           String categoryName,
                                           String departmentName) {
        return buildMismatchSamplesCsv(collectMismatchSamples(
                keyword,
                doctorName,
                reasonCode,
                categoryName,
                departmentName
        ));
    }

    @Override
    public byte[] exportFieldSamplesCsv(String fieldKey,
                                        String status,
                                        String keyword,
                                        String doctorName,
                                        String categoryName,
                                        String departmentName) {
        return buildFieldSamplesCsv(collectFieldSamples(
                fieldKey,
                status,
                keyword,
                doctorName,
                categoryName,
                departmentName
        ));
    }

    private List<AdminConsultationAiMismatchVO> collectMismatchSamples(String keyword,
                                                                       String doctorName,
                                                                       String reasonCode,
                                                                       String categoryName,
                                                                       String departmentName) {
        String keywordText = trimToNull(keyword);
        String doctorNameText = trimToNull(doctorName);
        String reasonCodeText = trimToNull(reasonCode);
        String categoryNameText = trimToNull(categoryName);
        String departmentNameText = trimToNull(departmentName);

        SampleQueryContext context = loadSampleQueryContext();
        if (context.records.isEmpty()) return List.of();

        return context.records.stream()
                .map(record -> buildMismatchItem(
                        record,
                        context.latestConclusionMap.get(record.getId()),
                        context.aiComparisonMap.get(record.getId())
                ))
                .filter(Objects::nonNull)
                .filter(item -> matchesMismatchSampleFilter(item, keywordText, doctorNameText, reasonCodeText, categoryNameText, departmentNameText))
                .sorted((a, b) -> compareSampleUpdateTime(a.getUpdateTime(), a.getConsultationId(), b.getUpdateTime(), b.getConsultationId()))
                .toList();
    }

    private List<AdminConsultationAiFieldSampleVO> collectFieldSamples(String fieldKey,
                                                                       String status,
                                                                       String keyword,
                                                                       String doctorName,
                                                                       String categoryName,
                                                                       String departmentName) {
        String normalizedFieldKey = normalizeFieldKey(fieldKey);
        String normalizedStatus = normalizeFieldSampleStatus(status);
        if (normalizedFieldKey == null || normalizedStatus == null) return List.of();
        String keywordText = trimToNull(keyword);
        String doctorNameText = trimToNull(doctorName);
        String categoryNameText = trimToNull(categoryName);
        String departmentNameText = trimToNull(departmentName);

        SampleQueryContext context = loadSampleQueryContext();
        if (context.records.isEmpty()) return List.of();

        return context.records.stream()
                .map(record -> buildFieldSample(
                        record,
                        context.latestConclusionMap.get(record.getId()),
                        context.aiComparisonMap.get(record.getId()),
                        normalizedFieldKey
                ))
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.getCompareStatus(), normalizedStatus))
                .filter(item -> matchesFieldSampleFilter(item, keywordText, doctorNameText, categoryNameText, departmentNameText))
                .sorted((a, b) -> compareSampleUpdateTime(a.getUpdateTime(), a.getConsultationId(), b.getUpdateTime(), b.getConsultationId()))
                .toList();
    }

    private SampleQueryContext loadSampleQueryContext() {
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) {
            return SampleQueryContext.empty();
        }

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, ConsultationDoctorConclusion> latestConclusionMap = loadLatestConclusionMap(consultationIds);
        Map<Integer, ConsultationAiComparisonVO> aiComparisonMap = buildAiComparisonMap(records, latestConclusionMap);
        return new SampleQueryContext(records, latestConclusionMap, aiComparisonMap);
    }

    private AdminConsultationDispatchSummaryVO emptyDispatchSummary(int overdueHours) {
        AdminConsultationDispatchSummaryVO summary = new AdminConsultationDispatchSummaryVO();
        summary.setTotalRecordCount(0);
        summary.setSuggestedCount(0);
        summary.setWaitingAcceptCount(0);
        summary.setClaimedBySuggestedCount(0);
        summary.setClaimedByOtherCount(0);
        summary.setDepartmentQueueCount(0);
        summary.setOverdueWaitingCount(0);
        summary.setOverdueThresholdHours(overdueHours);
        summary.setSuggestionCoverageText("0 / 0");
        summary.setSuggestedHitRateText("-");
        summary.setClaimedByOtherRateText("-");
        summary.setAverageClaimDurationText("-");
        summary.setDoctorBreakdown(List.of());
        summary.setRecentWaitingRecords(List.of());
        return summary;
    }

    private <T> List<T> paginate(List<T> items, int limit, int offset) {
        return items.stream()
                .skip(Math.max(offset, 0))
                .limit(Math.max(1, Math.min(limit, 50)))
                .toList();
    }

    private AdminConsultationDispatchWaitVO buildDispatchWaitItem(ConsultationRecord record,
                                                                  ConsultationSmartDispatchVO dispatch) {
        AdminConsultationDispatchWaitVO item = new AdminConsultationDispatchWaitVO();
        item.setConsultationId(record.getId());
        item.setConsultationNo(record.getConsultationNo());
        item.setPatientName(record.getPatientName());
        item.setCategoryName(record.getCategoryName());
        item.setDepartmentName(record.getDepartmentName());
        item.setSuggestedDoctorName(dispatch.getSuggestedDoctorName());
        item.setRecommendationReason(dispatch.getRecommendationReason());
        item.setCreateTime(record.getCreateTime());
        item.setWaitingDurationText(formatDurationMinutes(resolveWaitingMinutes(record.getCreateTime())));
        return item;
    }

    private AdminConsultationAiMismatchVO buildMismatchItem(ConsultationRecord record,
                                                            ConsultationDoctorConclusion conclusion,
                                                            ConsultationAiComparisonVO aiComparison) {
        if (record == null || conclusion == null || !Objects.equals(conclusion.getIsConsistentWithAi(), 0) || aiComparison == null) return null;

        AdminConsultationAiMismatchVO item = new AdminConsultationAiMismatchVO();
        item.setConsultationId(record.getId());
        item.setConsultationNo(record.getConsultationNo());
        item.setPatientName(record.getPatientName());
        item.setCategoryName(record.getCategoryName());
        item.setDepartmentName(record.getDepartmentName());
        item.setDoctorName(conclusion.getDoctorName());
        item.setAiConditionLevel(aiComparison.getAiConditionLevel());
        item.setDoctorConditionLevel(conclusion.getConditionLevel());
        item.setAiDisposition(aiComparison.getAiDisposition());
        item.setDoctorDisposition(conclusion.getDisposition());
        item.setAiFollowUpText(aiComparison.getAiFollowUpText());
        item.setDoctorFollowUpText(buildDoctorFollowUpText(conclusion));
        item.setAiReasonText(aiComparison.getAiReasonText());
        item.setMismatchReasonCodes(resolveMismatchReasonCodes(conclusion));
        item.setMismatchRemark(conclusion.getAiMismatchRemark());
        item.setUpdateTime(conclusion.getUpdateTime() != null ? conclusion.getUpdateTime() : conclusion.getCreateTime());
        return item;
    }

    private AdminConsultationAiFieldSampleVO buildFieldSample(ConsultationRecord record,
                                                              ConsultationDoctorConclusion conclusion,
                                                              ConsultationAiComparisonVO aiComparison,
                                                              String fieldKey) {
        if (record == null || aiComparison == null) return null;
        String compareStatus = compareStatusByFieldKey(aiComparison, fieldKey);
        if (compareStatus == null) return null;

        AdminConsultationAiFieldSampleVO item = new AdminConsultationAiFieldSampleVO();
        item.setFieldKey(fieldKey);
        item.setFieldLabel(fieldLabelOf(fieldKey));
        item.setCompareStatus(compareStatus);
        item.setConsultationId(record.getId());
        item.setConsultationNo(record.getConsultationNo());
        item.setPatientName(record.getPatientName());
        item.setCategoryName(record.getCategoryName());
        item.setDepartmentName(record.getDepartmentName());
        item.setDoctorName(conclusion == null ? null : conclusion.getDoctorName());
        item.setAiValueText(sampleAiValueText(aiComparison, fieldKey));
        item.setDoctorValueText(sampleDoctorValueText(conclusion, fieldKey));
        item.setMismatchReasonCodes(resolveMismatchReasonCodes(conclusion));
        item.setMismatchRemark(conclusion == null ? null : conclusion.getAiMismatchRemark());
        item.setUpdateTime(resolveFieldSampleUpdateTime(record, conclusion));
        return item;
    }

    private boolean matchesMismatchSampleFilter(AdminConsultationAiMismatchVO item,
                                                String keyword,
                                                String doctorName,
                                                String reasonCode,
                                                String categoryName,
                                                String departmentName) {
        if (item == null) return false;
        if (doctorName != null && !Objects.equals(trimToNull(item.getDoctorName()), doctorName)) return false;
        if (reasonCode != null && !resolveMismatchReasonCodes(item.getMismatchReasonCodes()).contains(reasonCode)) return false;
        if (categoryName != null && !Objects.equals(trimToNull(item.getCategoryName()), categoryName)) return false;
        if (departmentName != null && !Objects.equals(trimToNull(item.getDepartmentName()), departmentName)) return false;
        if (keyword == null) return true;
        return containsIgnoreCase(item.getConsultationNo(), keyword)
                || containsIgnoreCase(item.getPatientName(), keyword)
                || containsIgnoreCase(item.getDoctorName(), keyword)
                || containsIgnoreCase(item.getCategoryName(), keyword)
                || containsIgnoreCase(item.getDepartmentName(), keyword)
                || containsIgnoreCase(renderConditionLevel(item.getAiConditionLevel()), keyword)
                || containsIgnoreCase(renderConditionLevel(item.getDoctorConditionLevel()), keyword)
                || containsIgnoreCase(renderDisposition(item.getAiDisposition()), keyword)
                || containsIgnoreCase(renderDisposition(item.getDoctorDisposition()), keyword)
                || containsIgnoreCase(item.getAiFollowUpText(), keyword)
                || containsIgnoreCase(item.getDoctorFollowUpText(), keyword)
                || containsIgnoreCase(item.getAiReasonText(), keyword)
                || containsIgnoreCase(mismatchReasonText(item.getMismatchReasonCodes()), keyword)
                || containsIgnoreCase(item.getMismatchRemark(), keyword);
    }

    private boolean matchesFieldSampleFilter(AdminConsultationAiFieldSampleVO item,
                                             String keyword,
                                             String doctorName,
                                             String categoryName,
                                             String departmentName) {
        if (item == null) return false;
        if (doctorName != null && !Objects.equals(trimToNull(item.getDoctorName()), doctorName)) return false;
        if (categoryName != null && !Objects.equals(trimToNull(item.getCategoryName()), categoryName)) return false;
        if (departmentName != null && !Objects.equals(trimToNull(item.getDepartmentName()), departmentName)) return false;
        if (keyword == null) return true;
        return containsIgnoreCase(item.getConsultationNo(), keyword)
                || containsIgnoreCase(item.getPatientName(), keyword)
                || containsIgnoreCase(item.getDoctorName(), keyword)
                || containsIgnoreCase(item.getCategoryName(), keyword)
                || containsIgnoreCase(item.getDepartmentName(), keyword)
                || containsIgnoreCase(item.getAiValueText(), keyword)
                || containsIgnoreCase(item.getDoctorValueText(), keyword)
                || containsIgnoreCase(mismatchReasonText(item.getMismatchReasonCodes()), keyword)
                || containsIgnoreCase(item.getMismatchRemark(), keyword);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        String sourceText = trimToNull(source);
        String keywordText = trimToNull(keyword);
        if (sourceText == null || keywordText == null) return false;
        return sourceText.toLowerCase().contains(keywordText.toLowerCase());
    }

    private byte[] buildMismatchSamplesCsv(List<AdminConsultationAiMismatchVO> items) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        appendCsvRow(builder,
                "问诊ID",
                "问诊单号",
                "患者姓名",
                "问诊分类",
                "推荐科室",
                "处理医生",
                "AI病情等级",
                "医生病情等级",
                "AI处理去向",
                "医生处理去向",
                "AI随访安排",
                "医生随访安排",
                "AI推荐依据",
                "差异原因",
                "差异说明",
                "更新时间"
        );
        for (AdminConsultationAiMismatchVO item : items) {
            appendCsvRow(builder,
                    item.getConsultationId() == null ? null : String.valueOf(item.getConsultationId()),
                    item.getConsultationNo(),
                    item.getPatientName(),
                    item.getCategoryName(),
                    item.getDepartmentName(),
                    item.getDoctorName(),
                    renderConditionLevel(item.getAiConditionLevel()),
                    renderConditionLevel(item.getDoctorConditionLevel()),
                    renderDisposition(item.getAiDisposition()),
                    renderDisposition(item.getDoctorDisposition()),
                    item.getAiFollowUpText(),
                    item.getDoctorFollowUpText(),
                    item.getAiReasonText(),
                    mismatchReasonText(item.getMismatchReasonCodes()),
                    item.getMismatchRemark(),
                    formatCsvDate(item.getUpdateTime())
            );
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildFieldSamplesCsv(List<AdminConsultationAiFieldSampleVO> items) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        appendCsvRow(builder,
                "字段",
                "样本状态",
                "问诊ID",
                "问诊单号",
                "患者姓名",
                "问诊分类",
                "推荐科室",
                "处理医生",
                "AI取值",
                "医生取值",
                "差异原因",
                "差异说明",
                "更新时间"
        );
        for (AdminConsultationAiFieldSampleVO item : items) {
            appendCsvRow(builder,
                    item.getFieldLabel(),
                    renderFieldSampleStatus(item.getCompareStatus()),
                    item.getConsultationId() == null ? null : String.valueOf(item.getConsultationId()),
                    item.getConsultationNo(),
                    item.getPatientName(),
                    item.getCategoryName(),
                    item.getDepartmentName(),
                    item.getDoctorName(),
                    item.getAiValueText(),
                    item.getDoctorValueText(),
                    mismatchReasonText(item.getMismatchReasonCodes()),
                    item.getMismatchRemark(),
                    formatCsvDate(item.getUpdateTime())
            );
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendCsvRow(StringBuilder builder, String... columns) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(escapeCsv(columns[i]));
        }
        builder.append("\r\n");
    }

    private String escapeCsv(String value) {
        String text = value == null ? "" : value;
        boolean needQuote = text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n");
        String escaped = text.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    private String formatCsvDate(Date value) {
        if (value == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    private String renderFieldSampleStatus(String value) {
        return switch (normalizeCompareStatus(value)) {
            case "match" -> "一致";
            case "mismatch" -> "偏差";
            default -> "待补充";
        };
    }

    private int compareSampleUpdateTime(Date leftTime,
                                        Integer leftConsultationId,
                                        Date rightTime,
                                        Integer rightConsultationId) {
        if (leftTime == null && rightTime == null) {
            return Integer.compare(safeInt(rightConsultationId), safeInt(leftConsultationId));
        }
        if (leftTime == null) return 1;
        if (rightTime == null) return -1;
        return rightTime.compareTo(leftTime);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildDoctorFollowUpText(ConsultationDoctorConclusion conclusion) {
        if (conclusion == null || conclusion.getNeedFollowUp() == null) return null;
        if (conclusion.getNeedFollowUp() == 1) {
            return conclusion.getFollowUpWithinDays() == null ? "需要随访" : conclusion.getFollowUpWithinDays() + " 天内随访";
        }
        return "暂不需要随访";
    }

    private List<String> resolveMismatchReasonCodes(ConsultationDoctorConclusion conclusion) {
        if (conclusion == null) return List.of();
        List<String> reasonCodes = ConsultationAiMismatchReasonUtils.parseCodes(conclusion.getAiMismatchReasonsJson());
        if (reasonCodes.isEmpty() && trimToNull(conclusion.getAiMismatchRemark()) != null) {
            return List.of("other");
        }
        return reasonCodes;
    }

    private List<String> resolveMismatchReasonCodes(List<String> reasonCodes) {
        if (reasonCodes == null || reasonCodes.isEmpty()) return List.of();
        return reasonCodes.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String mismatchReasonText(List<String> reasonCodes) {
        List<String> normalizedCodes = resolveMismatchReasonCodes(reasonCodes);
        if (normalizedCodes.isEmpty()) return null;
        return normalizedCodes.stream()
                .map(ConsultationAiMismatchReasonUtils::labelOf)
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
    }

    private String formatPercent(int numerator, int denominator) {
        if (denominator <= 0) return "-";
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP) + "%";
    }

    private String formatAverageDuration(long totalMinutes, int sampleCount) {
        if (sampleCount <= 0 || totalMinutes <= 0) return "-";
        return formatDurationMinutes(Math.max(1L, Math.round((double) totalMinutes / sampleCount)));
    }

    private String formatDurationMinutes(Long minutes) {
        if (minutes == null || minutes <= 0) return "-";
        long duration = minutes;
        long days = duration / (24 * 60);
        duration %= 24 * 60;
        long hours = duration / 60;
        long remainMinutes = duration % 60;
        if (days > 0) {
            return remainMinutes > 0
                    ? days + "天 " + hours + "小时 " + remainMinutes + "分钟"
                    : days + "天 " + hours + "小时";
        }
        if (hours > 0) {
            return remainMinutes > 0 ? hours + "小时 " + remainMinutes + "分钟" : hours + "小时";
        }
        return duration + "分钟";
    }

    private Long resolveWaitingMinutes(Date createTime) {
        if (createTime == null) return null;
        long diff = System.currentTimeMillis() - createTime.getTime();
        if (diff <= 0) return 1L;
        return Math.max(1L, diff / 60000L);
    }

    private boolean isWaitingOverHours(Date createTime, int hours) {
        if (createTime == null || hours <= 0) return false;
        return System.currentTimeMillis() - createTime.getTime() >= hours * 60L * 60L * 1000L;
    }

    private int resolveWaitingOverdueHours(ConsultationDispatchConfig config) {
        return config == null || config.getWaitingOverdueHours() == null || config.getWaitingOverdueHours() <= 0
                ? 24
                : config.getWaitingOverdueHours();
    }

    private Long resolveClaimDurationMinutes(ConsultationRecord record,
                                             ConsultationDoctorAssignment assignment) {
        if (record == null || assignment == null || record.getCreateTime() == null) return null;
        Date claimTime = assignment.getClaimTime() != null ? assignment.getClaimTime() : assignment.getUpdateTime();
        if (claimTime == null) return null;
        long diff = claimTime.getTime() - record.getCreateTime().getTime();
        if (diff <= 0) return 1L;
        return Math.max(1L, diff / 60000L);
    }

    private Map<Integer, ConsultationDoctorAssignment> loadLatestAssignmentMap(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();
        Map<Integer, ConsultationDoctorAssignment> assignmentMap = new HashMap<>();
        consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> assignmentMap.putIfAbsent(item.getConsultationId(), item));
        return assignmentMap;
    }

    private Map<Integer, TriageResult> loadLatestTriageResultEntityMap(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();
        Map<Integer, TriageResult> triageResultMap = new HashMap<>();
        triageResultMapper.selectList(Wrappers.<TriageResult>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("is_final")
                        .orderByDesc("id"))
                .forEach(item -> triageResultMap.putIfAbsent(item.getConsultationId(), item));
        return triageResultMap;
    }

    private Map<Integer, ConsultationDoctorConclusion> loadLatestConclusionMap(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();
        Map<Integer, ConsultationDoctorConclusion> latestConclusionMap = new HashMap<>();
        consultationDoctorConclusionMapper.selectList(Wrappers.<ConsultationDoctorConclusion>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("id"))
                .forEach(item -> latestConclusionMap.putIfAbsent(item.getConsultationId(), item));
        return latestConclusionMap;
    }

    private Map<Integer, ConsultationAiComparisonVO> buildAiComparisonMap(List<ConsultationRecord> records,
                                                                          Map<Integer, ConsultationDoctorConclusion> latestConclusionMap) {
        if (records.isEmpty()) return Map.of();
        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, TriageSessionVO> triageSessionMap = loadTriageSessionMap(consultationIds);
        Map<Integer, TriageResultVO> triageResultMap = loadTriageResultMap(consultationIds);
        Map<Integer, ConsultationAiComparisonVO> result = new HashMap<>();
        for (ConsultationRecord record : records) {
            ConsultationDoctorConclusion conclusion = latestConclusionMap.get(record.getId());
            ConsultationDoctorConclusionVO conclusionVO = conclusion == null ? null : conclusion.asViewObject(ConsultationDoctorConclusionVO.class);
            ConsultationAiComparisonVO aiComparison = ConsultationAiComparisonUtils.build(
                    conclusionVO,
                    triageSessionMap.get(record.getId()),
                    triageResultMap.get(record.getId())
            );
            if (aiComparison != null) {
                result.put(record.getId(), aiComparison);
            }
        }
        return result;
    }

    private Map<Integer, TriageSessionVO> loadTriageSessionMap(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();
        Map<Integer, TriageSession> latestSessionMap = new HashMap<>();
        triageSessionMapper.selectList(Wrappers.<TriageSession>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("id"))
                .forEach(item -> latestSessionMap.putIfAbsent(item.getConsultationId(), item));
        if (latestSessionMap.isEmpty()) return Map.of();

        List<Integer> sessionIds = latestSessionMap.values().stream().map(TriageSession::getId).toList();
        Map<Integer, List<TriageMessageVO>> messageMap = new HashMap<>();
        triageMessageMapper.selectList(Wrappers.<TriageMessage>query()
                        .in("session_id", sessionIds)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .forEach(item -> messageMap.computeIfAbsent(item.getSessionId(), key -> new ArrayList<>())
                        .add(item.asViewObject(TriageMessageVO.class)));

        Map<Integer, TriageSessionVO> result = new HashMap<>();
        latestSessionMap.forEach((consultationId, session) -> result.put(consultationId, session.asViewObject(TriageSessionVO.class,
                vo -> vo.setMessages(messageMap.getOrDefault(session.getId(), List.of())))));
        return result;
    }

    private Map<Integer, TriageResultVO> loadTriageResultMap(List<Integer> consultationIds) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();
        Map<Integer, TriageResultVO> result = new HashMap<>();
        triageResultMapper.selectList(Wrappers.<TriageResult>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("is_final")
                        .orderByDesc("id"))
                .forEach(item -> result.putIfAbsent(item.getConsultationId(), item.asViewObject(TriageResultVO.class)));
        return result;
    }

    private List<AdminConsultationAiFieldVO> buildFieldBreakdown(List<ConsultationRecord> records,
                                                                 Map<Integer, ConsultationDoctorConclusion> latestConclusionMap,
                                                                 Map<Integer, ConsultationAiComparisonVO> aiComparisonMap) {
        return List.of(
                buildFieldBreakdownItem("condition_level", "病情等级", records, latestConclusionMap, aiComparisonMap, ConsultationAiComparisonVO::getConditionLevelStatus),
                buildFieldBreakdownItem("disposition", "处理去向", records, latestConclusionMap, aiComparisonMap, ConsultationAiComparisonVO::getDispositionStatus),
                buildFieldBreakdownItem("follow_up", "随访安排", records, latestConclusionMap, aiComparisonMap, ConsultationAiComparisonVO::getFollowUpStatus)
        );
    }

    private AdminConsultationAiFieldVO buildFieldBreakdownItem(String fieldKey,
                                                               String fieldLabel,
                                                               List<ConsultationRecord> records,
                                                               Map<Integer, ConsultationDoctorConclusion> latestConclusionMap,
                                                               Map<Integer, ConsultationAiComparisonVO> aiComparisonMap,
                                                               Function<ConsultationAiComparisonVO, String> statusResolver) {
        int totalCount = 0;
        int matchCount = 0;
        int mismatchCount = 0;
        int pendingCount = 0;
        Map<String, Integer> reasonCountMap = new HashMap<>();

        for (ConsultationRecord record : records) {
            ConsultationAiComparisonVO aiComparison = aiComparisonMap.get(record.getId());
            if (aiComparison == null) continue;
            totalCount++;
            String status = normalizeCompareStatus(statusResolver.apply(aiComparison));
            switch (status) {
                case "match" -> matchCount++;
                case "mismatch" -> {
                    mismatchCount++;
                    mergeMismatchReasons(reasonCountMap, latestConclusionMap.get(record.getId()));
                }
                default -> pendingCount++;
            }
        }

        AdminConsultationAiFieldVO item = new AdminConsultationAiFieldVO();
        item.setFieldKey(fieldKey);
        item.setFieldLabel(fieldLabel);
        item.setTotalCount(totalCount);
        item.setComparedCount(matchCount + mismatchCount);
        item.setMatchCount(matchCount);
        item.setMismatchCount(mismatchCount);
        item.setPendingCount(pendingCount);
        item.setMismatchRateText(matchCount + mismatchCount <= 0 ? "-" : formatPercent(mismatchCount, matchCount + mismatchCount));
        item.setMismatchReasonBreakdown(buildReasonBreakdown(reasonCountMap));
        return item;
    }

    private String compareStatusByFieldKey(ConsultationAiComparisonVO aiComparison, String fieldKey) {
        if (aiComparison == null) return null;
        return switch (fieldKey) {
            case "condition_level" -> normalizeFieldSampleStatus(aiComparison.getConditionLevelStatus());
            case "disposition" -> normalizeFieldSampleStatus(aiComparison.getDispositionStatus());
            case "follow_up" -> normalizeFieldSampleStatus(aiComparison.getFollowUpStatus());
            default -> null;
        };
    }

    private String fieldLabelOf(String fieldKey) {
        return switch (fieldKey) {
            case "condition_level" -> "病情等级";
            case "disposition" -> "处理去向";
            case "follow_up" -> "随访安排";
            default -> fieldKey;
        };
    }

    private String sampleAiValueText(ConsultationAiComparisonVO aiComparison, String fieldKey) {
        if (aiComparison == null) return null;
        return switch (fieldKey) {
            case "condition_level" -> renderConditionLevel(aiComparison.getAiConditionLevel());
            case "disposition" -> renderDisposition(aiComparison.getAiDisposition());
            case "follow_up" -> trimToNull(aiComparison.getAiFollowUpText());
            default -> null;
        };
    }

    private String sampleDoctorValueText(ConsultationDoctorConclusion conclusion, String fieldKey) {
        if (conclusion == null) return null;
        return switch (fieldKey) {
            case "condition_level" -> renderConditionLevel(conclusion.getConditionLevel());
            case "disposition" -> renderDisposition(conclusion.getDisposition());
            case "follow_up" -> buildDoctorFollowUpText(conclusion);
            default -> null;
        };
    }

    private String renderConditionLevel(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        return switch (text) {
            case "low" -> "轻度";
            case "medium" -> "中度";
            case "high" -> "较高风险";
            case "critical" -> "危急";
            default -> text;
        };
    }

    private String renderDisposition(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        return switch (text) {
            case "observe" -> "继续观察";
            case "online_followup" -> "线上随访";
            case "offline_visit" -> "线下就医";
            case "emergency" -> "立即急诊";
            default -> text;
        };
    }

    private Date resolveFieldSampleUpdateTime(ConsultationRecord record, ConsultationDoctorConclusion conclusion) {
        if (conclusion != null) {
            if (conclusion.getUpdateTime() != null) return conclusion.getUpdateTime();
            if (conclusion.getCreateTime() != null) return conclusion.getCreateTime();
        }
        return record == null ? null : record.getCreateTime();
    }

    private List<AdminConsultationAiDoctorVO> buildDoctorBreakdown(List<ConsultationDoctorConclusion> conclusions) {
        Map<String, DoctorAccumulator> doctorMap = new HashMap<>();
        for (ConsultationDoctorConclusion conclusion : conclusions) {
            if (conclusion == null) continue;
            DoctorAccumulator accumulator = doctorMap.computeIfAbsent(resolveDoctorKey(conclusion), key -> new DoctorAccumulator());
            if (accumulator.doctorId == null && conclusion.getDoctorId() != null) {
                accumulator.doctorId = conclusion.getDoctorId();
            }
            if (accumulator.doctorName == null) {
                accumulator.doctorName = normalizeGroupName(conclusion.getDoctorName(), "未记录医生");
            }
            if (accumulator.departmentName == null) {
                accumulator.departmentName = normalizeGroupName(conclusion.getDepartmentName(), "未记录科室");
            }
            accumulator.totalCount++;
            if (conclusion.getIsConsistentWithAi() == null) {
                accumulator.pendingCount++;
                continue;
            }
            accumulator.comparedCount++;
            if (conclusion.getIsConsistentWithAi() == 1) {
                accumulator.consistentCount++;
            } else if (conclusion.getIsConsistentWithAi() == 0) {
                accumulator.mismatchCount++;
                mergeMismatchReasons(accumulator.reasonCountMap, conclusion);
            }
        }

        return doctorMap.values().stream()
                .map(item -> {
                    AdminConsultationAiDoctorVO vo = new AdminConsultationAiDoctorVO();
                    vo.setDoctorId(item.doctorId);
                    vo.setDoctorName(item.doctorName);
                    vo.setDepartmentName(item.departmentName);
                    vo.setTotalCount(item.totalCount);
                    vo.setComparedCount(item.comparedCount);
                    vo.setConsistentCount(item.consistentCount);
                    vo.setMismatchCount(item.mismatchCount);
                    vo.setPendingCount(item.pendingCount);
                    vo.setCoverageText(item.comparedCount + " / " + item.totalCount);
                    vo.setConsistentRateText(item.comparedCount <= 0 ? "-" : formatPercent(item.consistentCount, item.comparedCount));
                    vo.setMismatchReasonBreakdown(buildReasonBreakdown(item.reasonCountMap));
                    return vo;
                })
                .sorted(Comparator
                        .comparing(AdminConsultationAiDoctorVO::getMismatchCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationAiDoctorVO::getComparedCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationAiDoctorVO::getTotalCount, Comparator.reverseOrder())
                .thenComparing(AdminConsultationAiDoctorVO::getDoctorName))
                .toList();
    }

    private List<AdminConsultationDispatchDoctorVO> buildDispatchDoctorBreakdown(Map<String, DispatchDoctorAccumulator> doctorMap) {
        return doctorMap.values().stream()
                .map(item -> {
                    AdminConsultationDispatchDoctorVO vo = new AdminConsultationDispatchDoctorVO();
                    vo.setDoctorId(item.doctorId);
                    vo.setDoctorName(item.doctorName);
                    vo.setDepartmentName(item.departmentName);
                    vo.setTotalSuggestedCount(item.totalSuggestedCount);
                    vo.setWaitingAcceptCount(item.waitingAcceptCount);
                    vo.setAcceptedCount(item.acceptedCount);
                    vo.setLostCount(item.lostCount);
                    vo.setHitRateText(formatPercent(item.acceptedCount, item.acceptedCount + item.lostCount));
                    vo.setAverageClaimDurationText(formatAverageDuration(item.acceptedClaimMinutes, item.acceptedClaimSampleCount));
                    return vo;
                })
                .sorted(Comparator
                        .comparing(AdminConsultationDispatchDoctorVO::getWaitingAcceptCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationDispatchDoctorVO::getLostCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationDispatchDoctorVO::getTotalSuggestedCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationDispatchDoctorVO::getDoctorName))
                .toList();
    }

    private List<AdminConsultationAiReasonVO> buildMismatchReasonBreakdown(List<ConsultationDoctorConclusion> conclusions) {
        Map<String, Integer> reasonCountMap = new HashMap<>();
        for (ConsultationDoctorConclusion conclusion : conclusions) {
            mergeMismatchReasons(reasonCountMap, conclusion);
        }
        return buildReasonBreakdown(reasonCountMap);
    }

    private List<AdminConsultationAiReasonVO> buildReasonBreakdown(Map<String, Integer> reasonCountMap) {
        return reasonCountMap.entrySet().stream()
                .map(entry -> {
                    AdminConsultationAiReasonVO item = new AdminConsultationAiReasonVO();
                    item.setReasonCode(entry.getKey());
                    item.setReasonLabel(ConsultationAiMismatchReasonUtils.labelOf(entry.getKey()));
                    item.setCount(entry.getValue());
                    return item;
                })
                .sorted(Comparator
                        .comparing(AdminConsultationAiReasonVO::getCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationAiReasonVO::getReasonLabel))
                .toList();
    }

    private void mergeMismatchReasons(Map<String, Integer> reasonCountMap,
                                      ConsultationDoctorConclusion conclusion) {
        if (conclusion == null || !Objects.equals(conclusion.getIsConsistentWithAi(), 0)) return;
        List<String> reasonCodes = resolveMismatchReasonCodes(conclusion);
        for (String reasonCode : reasonCodes) {
            reasonCountMap.merge(reasonCode, 1, Integer::sum);
        }
    }

    private List<AdminConsultationAiGroupVO> buildBreakdown(List<ConsultationRecord> records,
                                                            Map<Integer, ConsultationDoctorConclusion> latestConclusionMap,
                                                            Function<ConsultationRecord, String> groupResolver,
                                                            String fallbackGroupName) {
        Map<String, GroupAccumulator> groupMap = new HashMap<>();
        for (ConsultationRecord record : records) {
            String groupName = normalizeGroupName(groupResolver.apply(record), fallbackGroupName);
            GroupAccumulator accumulator = groupMap.computeIfAbsent(groupName, key -> new GroupAccumulator());
            accumulator.totalCount++;
            ConsultationDoctorConclusion conclusion = latestConclusionMap.get(record.getId());
            if (conclusion == null || conclusion.getIsConsistentWithAi() == null) {
                accumulator.pendingCount++;
                continue;
            }
            accumulator.comparedCount++;
            if (conclusion.getIsConsistentWithAi() == 1) {
                accumulator.consistentCount++;
            } else if (conclusion.getIsConsistentWithAi() == 0) {
                accumulator.mismatchCount++;
            }
        }

        return groupMap.entrySet().stream()
                .map(entry -> {
                    GroupAccumulator accumulator = entry.getValue();
                    AdminConsultationAiGroupVO item = new AdminConsultationAiGroupVO();
                    item.setGroupName(entry.getKey());
                    item.setTotalCount(accumulator.totalCount);
                    item.setComparedCount(accumulator.comparedCount);
                    item.setConsistentCount(accumulator.consistentCount);
                    item.setMismatchCount(accumulator.mismatchCount);
                    item.setPendingCount(accumulator.pendingCount);
                    item.setCoverageText(accumulator.comparedCount + " / " + accumulator.totalCount);
                    item.setConsistentRateText(accumulator.comparedCount <= 0 ? "-" : formatPercent(accumulator.consistentCount, accumulator.comparedCount));
                    return item;
                })
                .sorted(Comparator
                        .comparing(AdminConsultationAiGroupVO::getMismatchCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationAiGroupVO::getComparedCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationAiGroupVO::getTotalCount, Comparator.reverseOrder())
                        .thenComparing(AdminConsultationAiGroupVO::getGroupName))
                .toList();
    }

    private String normalizeGroupName(String value, String fallbackGroupName) {
        String text = trimToNull(value);
        return text == null ? fallbackGroupName : text;
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

    private String normalizeCompareStatus(String value) {
        String text = trimToNull(value);
        if (Objects.equals(text, "match")) return "match";
        if (Objects.equals(text, "mismatch")) return "mismatch";
        return "pending";
    }

    private String normalizeFieldKey(String value) {
        String text = trimToNull(value);
        if (Objects.equals(text, "condition_level")) return "condition_level";
        if (Objects.equals(text, "disposition")) return "disposition";
        if (Objects.equals(text, "follow_up")) return "follow_up";
        return null;
    }

    private String normalizeFieldSampleStatus(String value) {
        String text = trimToNull(value);
        if (Objects.equals(text, "mismatch")) return "mismatch";
        if (Objects.equals(text, "pending")) return "pending";
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String resolveDoctorKey(ConsultationDoctorConclusion conclusion) {
        if (conclusion == null) return "doctor:unknown";
        if (conclusion.getDoctorId() != null) {
            return "doctor:" + conclusion.getDoctorId();
        }
        String doctorName = trimToNull(conclusion.getDoctorName());
        if (doctorName != null) {
            return "doctor-name:" + doctorName;
        }
        String departmentName = trimToNull(conclusion.getDepartmentName());
        if (departmentName != null) {
            return "doctor-department:" + departmentName;
        }
        return "doctor:unknown";
    }

    private String resolveDispatchDoctorKey(ConsultationSmartDispatchVO dispatch) {
        if (dispatch == null) return "doctor:unknown";
        if (dispatch.getSuggestedDoctorId() != null) {
            return "doctor:" + dispatch.getSuggestedDoctorId();
        }
        String doctorName = trimToNull(dispatch.getSuggestedDoctorName());
        return doctorName == null ? "doctor:unknown" : "doctor-name:" + doctorName;
    }

    private static final class SampleQueryContext {
        final List<ConsultationRecord> records;
        final Map<Integer, ConsultationDoctorConclusion> latestConclusionMap;
        final Map<Integer, ConsultationAiComparisonVO> aiComparisonMap;

        private SampleQueryContext(List<ConsultationRecord> records,
                                   Map<Integer, ConsultationDoctorConclusion> latestConclusionMap,
                                   Map<Integer, ConsultationAiComparisonVO> aiComparisonMap) {
            this.records = records;
            this.latestConclusionMap = latestConclusionMap;
            this.aiComparisonMap = aiComparisonMap;
        }

        private static SampleQueryContext empty() {
            return new SampleQueryContext(List.of(), Map.of(), Map.of());
        }
    }

    private static final class GroupAccumulator {
        int totalCount;
        int comparedCount;
        int consistentCount;
        int mismatchCount;
        int pendingCount;
    }

    private static final class DoctorAccumulator {
        Integer doctorId;
        String doctorName;
        String departmentName;
        int totalCount;
        int comparedCount;
        int consistentCount;
        int mismatchCount;
        int pendingCount;
        Map<String, Integer> reasonCountMap = new HashMap<>();
    }

    private static final class DispatchDoctorAccumulator {
        Integer doctorId;
        String doctorName;
        String departmentName;
        int totalSuggestedCount;
        int waitingAcceptCount;
        int acceptedCount;
        int lostCount;
        long acceptedClaimMinutes;
        int acceptedClaimSampleCount;
    }
}
