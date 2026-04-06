package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.service.ConsultationDispatchConfigService;
import cn.gugufish.service.ConsultationDoctorRecommendationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConsultationDoctorRecommendationServiceImpl implements ConsultationDoctorRecommendationService {

    private static final SimpleDateFormat SCHEDULE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Resource
    ConsultationDispatchConfigService consultationDispatchConfigService;

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    DoctorScheduleMapper doctorScheduleMapper;

    @Resource
    DoctorServiceTagMapper doctorServiceTagMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Override
    public List<ConsultationRecommendDoctorVO> recommendDoctors(ConsultationRecord record) {
        return recommendDoctors(record, consultationDispatchConfigService.getConfig());
    }

    @Override
    public List<ConsultationRecommendDoctorVO> recommendDoctors(ConsultationRecord record, ConsultationDispatchConfig config) {
        if (record == null || record.getDepartmentId() == null) return List.of();
        List<Doctor> doctors = doctorMapper.selectList(Wrappers.<Doctor>query()
                .eq("department_id", record.getDepartmentId())
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        if (doctors.isEmpty()) return List.of();

        List<Integer> doctorIds = doctors.stream().map(Doctor::getId).toList();
        Date today = java.sql.Date.valueOf(LocalDate.now());
        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(Wrappers.<DoctorSchedule>query()
                        .in("doctor_id", doctorIds)
                        .eq("status", 1)
                        .ge("schedule_date", today))
                .stream()
                .sorted(Comparator
                        .comparing(DoctorSchedule::getScheduleDate)
                        .thenComparingInt(item -> scheduleTimeOrder(item.getTimePeriod()))
                        .thenComparingInt(DoctorSchedule::getId))
                .toList();

        Map<Integer, DoctorSchedule> nextAvailableScheduleMap = new HashMap<>();
        Map<Integer, DoctorSchedule> nextScheduleMap = new HashMap<>();
        for (DoctorSchedule schedule : schedules) {
            nextScheduleMap.putIfAbsent(schedule.getDoctorId(), schedule);
            if (remainingCapacity(schedule) > 0) {
                nextAvailableScheduleMap.putIfAbsent(schedule.getDoctorId(), schedule);
            }
        }

        Map<Integer, List<String>> tagMap = doctorServiceTagMapper.selectList(Wrappers.<DoctorServiceTag>query()
                        .in("doctor_id", doctorIds)
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(DoctorServiceTag::getDoctorId,
                        Collectors.mapping(DoctorServiceTag::getTagName, Collectors.toList())));
        Map<Integer, Integer> activeConsultationCountMap = loadActiveConsultationCountMap(doctorIds);

        return doctors.stream()
                .map(item -> buildDoctorRecommendationSnapshot(
                        config,
                        record,
                        item,
                        nextAvailableScheduleMap.getOrDefault(item.getId(), nextScheduleMap.get(item.getId())),
                        nextAvailableScheduleMap.containsKey(item.getId()),
                        tagMap.getOrDefault(item.getId(), List.of()),
                        activeConsultationCountMap.getOrDefault(item.getId(), 0)
                ))
                .sorted(Comparator
                        .comparingInt(DoctorRecommendationSnapshot::recommendationScore).reversed()
                        .thenComparing(DoctorRecommendationSnapshot::hasAvailableSchedule, Comparator.reverseOrder())
                        .thenComparingLong(DoctorRecommendationSnapshot::scheduleRank)
                        .thenComparingInt(item -> defaultSort(item.doctor().getSort()))
                        .thenComparingInt(item -> item.doctor().getId()))
                .limit(resolveRecommendDoctorLimit(config))
                .map(this::convertRecommendationVO)
                .toList();
    }

    private ConsultationRecommendDoctorVO convertRecommendationVO(DoctorRecommendationSnapshot item) {
        DoctorSchedule schedule = item.schedule();
        ConsultationRecommendDoctorVO vo = new ConsultationRecommendDoctorVO();
        vo.setId(item.doctor().getId());
        vo.setName(item.doctor().getName());
        vo.setTitle(item.doctor().getTitle());
        vo.setPhoto(item.doctor().getPhoto());
        vo.setExpertise(item.doctor().getExpertise());
        vo.setIntroduction(item.doctor().getIntroduction());
        vo.setNextScheduleText(buildScheduleText(schedule));
        vo.setRemainingCapacity(schedule == null ? null : remainingCapacity(schedule));
        vo.setActiveConsultationCount(item.activeConsultationCount());
        vo.setRecommendationScore(item.recommendationScore());
        vo.setRecommendationSummary(item.recommendationSummary());
        vo.setRecommendationReasons(item.recommendationReasons());
        vo.setServiceTags(item.serviceTags());
        vo.setMatchedServiceTags(item.matchedServiceTags());
        return vo;
    }

    private DoctorRecommendationSnapshot buildDoctorRecommendationSnapshot(ConsultationDispatchConfig config,
                                                                          ConsultationRecord record,
                                                                          Doctor doctor,
                                                                          DoctorSchedule schedule,
                                                                          boolean hasAvailableSchedule,
                                                                          List<String> serviceTags,
                                                                          int activeConsultationCount) {
        List<String> matchedServiceTags = matchServiceTags(record, serviceTags, config);
        int remainingCapacity = schedule == null ? 0 : remainingCapacity(schedule);
        int maxCapacity = schedule == null ? 0 : defaultInt(schedule.getMaxCapacity());
        boolean highPriority = isHighPriorityConsultation(record);

        int score = scaleScore(
                resolveVisitTypeScore(record.getTriageActionType(), schedule == null ? null : schedule.getVisitType()),
                config == null ? null : config.getVisitTypeWeight()
        ) + scaleScore(
                resolveScheduleScore(schedule, hasAvailableSchedule, highPriority),
                config == null ? null : config.getScheduleWeight()
        ) + scaleScore(
                resolveCapacityScore(remainingCapacity, maxCapacity, hasAvailableSchedule, highPriority),
                config == null ? null : config.getCapacityWeight()
        ) + scaleScore(
                resolveWorkloadScore(activeConsultationCount, highPriority),
                config == null ? null : config.getWorkloadWeight()
        ) + scaleScore(
                Math.min(matchedServiceTags.size(), resolveMaxMatchedTags(config)) * resolveTagMatchScorePerHit(config),
                config == null ? null : config.getTagMatchWeight()
        );
        score = Math.max(resolveMinRecommendationScore(config), Math.min(score, resolveMaxRecommendationScore(config)));

        List<String> reasons = buildDoctorRecommendationReasons(
                record,
                schedule,
                hasAvailableSchedule,
                remainingCapacity,
                activeConsultationCount,
                matchedServiceTags
        );
        String summary = abbreviate(String.join("；", reasons), 120);
        long scheduleRank = schedule == null
                ? Long.MAX_VALUE
                : scheduleDelayDays(schedule.getScheduleDate()) * 10 + scheduleTimeOrder(schedule.getTimePeriod());
        return new DoctorRecommendationSnapshot(
                doctor,
                schedule,
                hasAvailableSchedule,
                activeConsultationCount,
                score,
                scheduleRank,
                serviceTags,
                matchedServiceTags,
                reasons,
                summary
        );
    }

    private String buildScheduleText(DoctorSchedule schedule) {
        if (schedule == null) return "暂未配置后续排班";
        return SCHEDULE_DATE_FORMAT.format(schedule.getScheduleDate())
                + " "
                + timePeriodLabel(schedule.getTimePeriod())
                + " / "
                + visitTypeLabel(schedule.getVisitType())
                + " / "
                + (remainingCapacity(schedule) > 0 ? "剩余 " + remainingCapacity(schedule) + " 号" : "当前号源已满");
    }

    private int remainingCapacity(DoctorSchedule schedule) {
        return Math.max(defaultInt(schedule.getMaxCapacity()) - defaultInt(schedule.getUsedCapacity()), 0);
    }

    private Map<Integer, Integer> loadActiveConsultationCountMap(List<Integer> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) return Map.of();
        List<Integer> activeConsultationIds = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                        .in("status", List.of("triaged", "processing")))
                .stream()
                .map(ConsultationRecord::getId)
                .toList();
        if (activeConsultationIds.isEmpty()) return Map.of();

        return consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", activeConsultationIds)
                        .in("doctor_id", doctorIds)
                        .eq("status", "claimed"))
                .stream()
                .collect(Collectors.groupingBy(
                        ConsultationDoctorAssignment::getDoctorId,
                        Collectors.collectingAndThen(
                                Collectors.mapping(ConsultationDoctorAssignment::getConsultationId, Collectors.toSet()),
                                Set::size
                        )
                ));
    }

    private boolean isHighPriorityConsultation(ConsultationRecord record) {
        String actionType = trimToNull(record == null ? null : record.getTriageActionType());
        if ("emergency".equalsIgnoreCase(actionType) || "offline".equalsIgnoreCase(actionType)) return true;
        String levelCode = trimToNull(record == null ? null : record.getTriageLevelCode());
        if (levelCode == null) return false;
        String text = levelCode.toUpperCase();
        return text.contains("EMERGENCY") || text.contains("URGENT") || text.contains("HIGH") || text.contains("RED");
    }

    private int resolveVisitTypeScore(String triageActionType, String visitType) {
        String actionType = trimToNull(triageActionType);
        String scheduleVisitType = trimToNull(visitType);
        if (scheduleVisitType == null) return 6;
        if (actionType == null) return 10;

        actionType = actionType.toLowerCase();
        scheduleVisitType = scheduleVisitType.toLowerCase();
        return switch (actionType) {
            case "emergency", "offline" -> switch (scheduleVisitType) {
                case "offline" -> 28;
                case "both" -> 26;
                case "online" -> 10;
                case "followup" -> 8;
                default -> 6;
            };
            case "online" -> switch (scheduleVisitType) {
                case "online" -> 24;
                case "both" -> 22;
                case "followup" -> 16;
                case "offline" -> 12;
                default -> 8;
            };
            case "observe" -> switch (scheduleVisitType) {
                case "online" -> 22;
                case "followup" -> 20;
                case "both" -> 18;
                case "offline" -> 10;
                default -> 8;
            };
            default -> switch (scheduleVisitType) {
                case "both" -> 18;
                case "online", "offline" -> 16;
                case "followup" -> 12;
                default -> 8;
            };
        };
    }

    private int resolveScheduleScore(DoctorSchedule schedule,
                                     boolean hasAvailableSchedule,
                                     boolean highPriority) {
        if (schedule == null) return 2;
        long delayDays = scheduleDelayDays(schedule.getScheduleDate());
        int timeOrder = scheduleTimeOrder(schedule.getTimePeriod());

        if (hasAvailableSchedule) {
            if (highPriority) {
                if (delayDays == 0 && timeOrder <= 2) return 30;
                if (delayDays == 0) return 28;
                if (delayDays <= 1) return 24;
                if (delayDays <= 3) return 18;
                return 12;
            }
            if (delayDays == 0) return 24;
            if (delayDays <= 1) return 21;
            if (delayDays <= 3) return 18;
            if (delayDays <= 7) return 12;
            return 8;
        }

        if (delayDays <= 1) return 8;
        if (delayDays <= 3) return 6;
        return 4;
    }

    private int resolveCapacityScore(int remainingCapacity,
                                     int maxCapacity,
                                     boolean hasAvailableSchedule,
                                     boolean highPriority) {
        if (!hasAvailableSchedule) return maxCapacity > 0 ? 2 : 0;
        if (maxCapacity <= 0) return highPriority ? 8 : 6;

        double ratio = Math.min(Math.max((double) remainingCapacity / maxCapacity, 0D), 1D);
        int base = highPriority ? 8 : 6;
        int extra = (int) Math.round(ratio * (highPriority ? 10 : 8));
        return base + extra;
    }

    private int resolveWorkloadScore(int activeConsultationCount,
                                     boolean highPriority) {
        return switch (Math.max(activeConsultationCount, 0)) {
            case 0 -> highPriority ? 16 : 14;
            case 1 -> highPriority ? 14 : 12;
            case 2 -> highPriority ? 12 : 10;
            case 3 -> highPriority ? 10 : 8;
            case 4, 5 -> highPriority ? 8 : 6;
            default -> highPriority ? 4 : 3;
        };
    }

    private List<String> buildDoctorRecommendationReasons(ConsultationRecord record,
                                                          DoctorSchedule schedule,
                                                          boolean hasAvailableSchedule,
                                                          int remainingCapacity,
                                                          int activeConsultationCount,
                                                          List<String> matchedServiceTags) {
        List<String> reasons = new ArrayList<>();
        String visitTypeReason = buildVisitTypeReason(record == null ? null : record.getTriageActionType(), schedule == null ? null : schedule.getVisitType());
        if (visitTypeReason != null) reasons.add(visitTypeReason);

        if (schedule == null) {
            reasons.add("暂未配置后续排班，当前优先参考科室与工作负载");
        } else if (hasAvailableSchedule) {
            reasons.add("近期排班可接诊：" + buildScheduleText(schedule));
        } else {
            reasons.add("已配置排班，但最近号源较紧：" + buildScheduleText(schedule));
        }

        reasons.add(activeConsultationCount <= 1
                ? "当前待处理量较低"
                : activeConsultationCount <= 3
                ? "当前待处理量适中（" + activeConsultationCount + " 单）"
                : "当前待处理量较高（" + activeConsultationCount + " 单）");

        if (!matchedServiceTags.isEmpty()) {
            reasons.add("服务标签匹配：" + abbreviate(String.join("、", matchedServiceTags), 28));
        } else if (remainingCapacity > 0) {
            reasons.add("剩余号源 " + remainingCapacity + " 个");
        }

        return reasons.stream()
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private String buildVisitTypeReason(String triageActionType,
                                        String visitType) {
        String actionType = trimToNull(triageActionType);
        String scheduleVisitType = trimToNull(visitType);
        if (actionType == null || scheduleVisitType == null) return null;
        actionType = actionType.toLowerCase();
        scheduleVisitType = scheduleVisitType.toLowerCase();

        return switch (actionType) {
            case "emergency", "offline" -> switch (scheduleVisitType) {
                case "offline", "both" -> "线下接诊方式更匹配当前病情";
                case "online" -> "可先线上接诊，再决定是否转线下";
                default -> "已有接诊方式可参考";
            };
            case "online" -> switch (scheduleVisitType) {
                case "online", "both" -> "线上接诊方式更匹配当前咨询";
                case "followup" -> "复诊随访方式更接近当前需求";
                default -> "已有可参考排班";
            };
            case "observe" -> switch (scheduleVisitType) {
                case "online", "followup", "both" -> "继续观察场景下便于线上跟进";
                default -> "已有可参考排班";
            };
            default -> null;
        };
    }

    private List<String> matchServiceTags(ConsultationRecord record,
                                          List<String> serviceTags,
                                          ConsultationDispatchConfig config) {
        if (record == null || serviceTags == null || serviceTags.isEmpty()) return List.of();
        String text = normalizeMatchText(String.join(" ",
                trimToNull(record.getCategoryName()) == null ? "" : record.getCategoryName(),
                trimToNull(record.getTitle()) == null ? "" : record.getTitle(),
                trimToNull(record.getChiefComplaint()) == null ? "" : record.getChiefComplaint(),
                trimToNull(record.getTriageRuleSummary()) == null ? "" : record.getTriageRuleSummary()
        ));
        if (text == null) return List.of();

        return serviceTags.stream()
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .filter(item -> {
                    String normalized = normalizeMatchText(item);
                    return normalized != null && normalized.length() >= 2 && text.contains(normalized);
                })
                .distinct()
                .limit(resolveMaxMatchedTags(config))
                .toList();
    }

    private int scaleScore(int score, Integer weight) {
        if (score <= 0) return 0;
        int normalizedWeight = weight == null || weight < 0 ? 100 : weight;
        return (int) Math.round(score * normalizedWeight / 100D);
    }

    private int resolveRecommendDoctorLimit(ConsultationDispatchConfig config) {
        return normalizePositive(config == null ? null : config.getRecommendDoctorLimit(), 3);
    }

    private int resolveTagMatchScorePerHit(ConsultationDispatchConfig config) {
        return normalizeNonNegative(config == null ? null : config.getTagMatchScorePerHit(), 4);
    }

    private int resolveMaxMatchedTags(ConsultationDispatchConfig config) {
        return normalizePositive(config == null ? null : config.getMaxMatchedTags(), 3);
    }

    private int resolveMinRecommendationScore(ConsultationDispatchConfig config) {
        return normalizeNonNegative(config == null ? null : config.getMinRecommendationScore(), 24);
    }

    private int resolveMaxRecommendationScore(ConsultationDispatchConfig config) {
        int minScore = resolveMinRecommendationScore(config);
        int maxScore = normalizePositive(config == null ? null : config.getMaxRecommendationScore(), 99);
        return Math.max(minScore, maxScore);
    }

    private String normalizeMatchText(String value) {
        String text = trimToNull(value);
        return text == null ? null : text.replaceAll("\\s+", "").toLowerCase();
    }

    private long scheduleDelayDays(Date scheduleDate) {
        if (scheduleDate == null) return Long.MAX_VALUE;
        LocalDate targetDate = new java.sql.Date(scheduleDate.getTime()).toLocalDate();
        return Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(), targetDate));
    }

    private int scheduleTimeOrder(String timePeriod) {
        return switch (trimToNull(timePeriod) == null ? "" : timePeriod.toLowerCase()) {
            case "morning" -> 1;
            case "afternoon" -> 2;
            case "evening" -> 3;
            default -> 9;
        };
    }

    private String timePeriodLabel(String timePeriod) {
        return switch (trimToNull(timePeriod) == null ? "" : timePeriod.toLowerCase()) {
            case "morning" -> "上午";
            case "afternoon" -> "下午";
            case "evening" -> "晚上";
            default -> "待定时段";
        };
    }

    private String visitTypeLabel(String visitType) {
        return switch (trimToNull(visitType) == null ? "" : visitType.toLowerCase()) {
            case "online" -> "线上问诊";
            case "offline" -> "线下面诊";
            case "followup" -> "复诊随访";
            case "both" -> "线上 / 线下";
            default -> "待定方式";
        };
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(maxLength - 3, 0)) + "...";
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int defaultSort(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private int normalizePositive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private int normalizeNonNegative(Integer value, int defaultValue) {
        return value == null || value < 0 ? defaultValue : value;
    }

    private record DoctorRecommendationSnapshot(Doctor doctor,
                                                DoctorSchedule schedule,
                                                boolean hasAvailableSchedule,
                                                int activeConsultationCount,
                                                int recommendationScore,
                                                long scheduleRank,
                                                List<String> serviceTags,
                                                List<String> matchedServiceTags,
                                                List<String> recommendationReasons,
                                                String recommendationSummary) {
    }
}
