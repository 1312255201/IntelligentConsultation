package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.vo.response.AdminConsultationAiDoctorVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiGroupVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiMismatchVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiReasonVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiSummaryVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.TriageRuleHitLogVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.service.ConsultationDoctorAssignmentQueryService;
import cn.gugufish.service.ConsultationRecordAdminService;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import cn.gugufish.utils.ConsultationAiComparisonUtils;
import cn.gugufish.utils.ConsultationAiMismatchReasonUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
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
    public List<AdminConsultationRecordVO> listRecords() {
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) return List.of();

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, ConsultationDoctorAssignment> assignmentMap = new HashMap<>();
        consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", consultationIds))
                .stream()
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
            summary.setDoctorBreakdown(List.of());
            summary.setMismatchReasonBreakdown(List.of());
            summary.setRecentMismatchRecords(List.of());
            return summary;
        }

        Map<Integer, ConsultationRecord> recordMap = new HashMap<>();
        records.forEach(item -> recordMap.put(item.getId(), item));

        List<ConsultationDoctorConclusion> conclusionList = consultationDoctorConclusionMapper.selectList(Wrappers.<ConsultationDoctorConclusion>query()
                .orderByDesc("id"));
        Map<Integer, ConsultationDoctorConclusion> latestConclusionMap = new HashMap<>();
        conclusionList.forEach(item -> latestConclusionMap.putIfAbsent(item.getConsultationId(), item));

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
                .map(item -> buildMismatchItem(recordMap.get(item.getConsultationId()), item))
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
        var doctorFollowUps = consultationDoctorFollowUpQueryService.listByConsultationId(id);
        var triageSession = triageSessionQueryService.detailByConsultationId(id);
        var triageResult = triageResultQueryService.detailByConsultationId(id);
        var triageFeedback = triageFeedbackQueryService.detailByConsultationId(id);

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

    private AdminConsultationAiMismatchVO buildMismatchItem(ConsultationRecord record,
                                                            ConsultationDoctorConclusion conclusion) {
        if (record == null || conclusion == null) return null;
        ConsultationDoctorConclusionVO conclusionVO = conclusion.asViewObject(ConsultationDoctorConclusionVO.class);
        var triageSession = triageSessionQueryService.detailByConsultationId(record.getId());
        var triageResult = triageResultQueryService.detailByConsultationId(record.getId());
        var aiComparison = ConsultationAiComparisonUtils.build(conclusionVO, triageSession, triageResult);
        if (aiComparison == null) return null;

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
        item.setMismatchReasonCodes(ConsultationAiMismatchReasonUtils.parseCodes(conclusion.getAiMismatchReasonsJson()));
        item.setMismatchRemark(conclusion.getAiMismatchRemark());
        item.setUpdateTime(conclusion.getUpdateTime() != null ? conclusion.getUpdateTime() : conclusion.getCreateTime());
        return item;
    }

    private String buildDoctorFollowUpText(ConsultationDoctorConclusion conclusion) {
        if (conclusion == null || conclusion.getNeedFollowUp() == null) return null;
        if (conclusion.getNeedFollowUp() == 1) {
            return conclusion.getFollowUpWithinDays() == null ? "需要随访" : conclusion.getFollowUpWithinDays() + " 天内随访";
        }
        return "暂不需要随访";
    }

    private String formatPercent(int numerator, int denominator) {
        if (denominator <= 0) return "-";
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP) + "%";
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
        List<String> reasonCodes = ConsultationAiMismatchReasonUtils.parseCodes(conclusion.getAiMismatchReasonsJson());
        if (reasonCodes.isEmpty() && trimToNull(conclusion.getAiMismatchRemark()) != null) {
            reasonCodes = List.of("other");
        }
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
}
