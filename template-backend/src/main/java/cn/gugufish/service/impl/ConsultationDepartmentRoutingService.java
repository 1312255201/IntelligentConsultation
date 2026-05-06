package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageAdvice;
import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.service.ConsultationDoctorRecommendationService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConsultationDepartmentRoutingService {

    private final ConsultationRecordMapper consultationRecordMapper;
    private final TriageSessionMapper triageSessionMapper;
    private final TriageResultMapper triageResultMapper;
    private final DepartmentMapper departmentMapper;
    private final ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;
    private final ConsultationDoctorRecommendationService consultationDoctorRecommendationService;

    public ConsultationDepartmentRoutingService(ConsultationRecordMapper consultationRecordMapper,
                                                TriageSessionMapper triageSessionMapper,
                                                TriageResultMapper triageResultMapper,
                                                DepartmentMapper departmentMapper,
                                                ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper,
                                                ConsultationDoctorRecommendationService consultationDoctorRecommendationService) {
        this.consultationRecordMapper = consultationRecordMapper;
        this.triageSessionMapper = triageSessionMapper;
        this.triageResultMapper = triageResultMapper;
        this.departmentMapper = departmentMapper;
        this.consultationDoctorAssignmentMapper = consultationDoctorAssignmentMapper;
        this.consultationDoctorRecommendationService = consultationDoctorRecommendationService;
    }

    public ConsultationRecord applyAiRecommendedDepartment(ConsultationRecord record,
                                                           TriageSession session,
                                                           TriageResult triageResult,
                                                           AiTriageAdvice advice) {
        if (record == null || session == null || advice == null) return record;
        if (!canAutoRoute(record)) return record;
        if (!isGeneralEntryDepartment(record)) return record;
        if (isClaimed(record.getId())) return record;

        Department targetDepartment = matchDepartment(advice.getRecommendedDepartmentName());
        if (targetDepartment == null) return record;
        if (Objects.equals(targetDepartment.getId(), record.getDepartmentId())) return record;

        Date now = new Date();
        record.setDepartmentId(targetDepartment.getId());
        record.setDepartmentName(targetDepartment.getName());
        record.setUpdateTime(now);
        consultationRecordMapper.updateById(record);

        session.setDepartmentId(targetDepartment.getId());
        session.setDepartmentName(targetDepartment.getName());
        session.setTriageSummary(buildSessionSummary(record, advice));
        session.setUpdateTime(now);
        triageSessionMapper.updateById(session);

        TriageResult currentResult = triageResult != null ? triageResult : latestTriageResult(record.getId());
        if (currentResult != null) {
            List<ConsultationRecommendDoctorVO> candidates = consultationDoctorRecommendationService.recommendDoctors(record);
            ConsultationRecommendDoctorVO firstDoctor = candidates.isEmpty() ? null : candidates.get(0);

            currentResult.setDepartmentId(targetDepartment.getId());
            currentResult.setDepartmentName(targetDepartment.getName());
            currentResult.setDoctorId(firstDoctor == null ? null : firstDoctor.getId());
            currentResult.setDoctorName(firstDoctor == null ? null : firstDoctor.getName());
            currentResult.setDoctorCandidatesJson(JSON.toJSONString(candidates));
            currentResult.setReasonText(mergeReasonText(currentResult.getReasonText(), advice, targetDepartment, firstDoctor));
            currentResult.setUpdateTime(now);
            triageResultMapper.updateById(currentResult);
        }

        log.info("Consultation {} rerouted to department {} by AI recommendation",
                record.getId(),
                targetDepartment.getName());
        return record;
    }

    public String buildDepartmentSelectionGuidance(ConsultationRecord record) {
        List<Department> departments = loadEnabledDepartments();
        String options = buildDepartmentOptionsText(departments);
        if (!StringUtils.hasText(options)) {
            options = "- 全科门诊：综合入口与兜底科室";
        }

        if (record == null) {
            return """
                    recommendedDepartmentName 只能填写系统中真实存在的科室名称，不允许编造新的科室。
                    如果无法判断更合适的专科，请返回“全科门诊”作为兜底结果。
                    当前系统科室如下：
                    %s
                    """.formatted(options);
        }

        String currentDepartmentName = safeText(record.getDepartmentName(), "全科门诊");
        if (!isGeneralEntryDepartment(record)) {
            return """
                    当前问诊已进入具体科室“%s”。
                    recommendedDepartmentName 只能从下列系统真实科室中选择；若没有充分依据改派到其他专科，请保持返回“%s”。
                    当前系统科室如下：
                    %s
                    """.formatted(currentDepartmentName, currentDepartmentName, options);
        }

        return """
                当前入口属于综合分诊入口。
                recommendedDepartmentName 只能从下列系统真实科室名称中选择，不允许编造列表之外的科室名称：
                %s

                选择规则：
                1. 若能明确判断更合适的专科，必须直接返回该科室的准确名称。
                2. 若现有信息不足以判断，必须返回“全科门诊”作为兜底结果。
                3. 不要返回“综合门诊”“推荐科室：内科”“建议去儿科门诊”等带修饰文本，只返回科室名称本身。
                """.formatted(options);
    }

    public String resolveDepartmentSelectionMode(ConsultationRecord record) {
        return isGeneralEntryDepartment(record) ? "general_entry" : "locked_department";
    }

    public List<Map<String, Object>> buildAvailableDepartmentPayload(ConsultationRecord record) {
        Integer currentDepartmentId = record == null ? null : record.getDepartmentId();
        return loadEnabledDepartments().stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("name", safeText(item.getName(), "未命名科室"));
                    if (StringUtils.hasText(item.getCode())) {
                        payload.put("code", item.getCode().trim());
                    }
                    String description = trimToNull(item.getDescription());
                    if (description != null) {
                        payload.put("description", abbreviate(description, 48));
                    }
                    if (isGeneralEntryDepartment(item)) {
                        payload.put("fallback", 1);
                    }
                    if (Objects.equals(currentDepartmentId, item.getId())) {
                        payload.put("current", 1);
                    }
                    return payload;
                })
                .toList();
    }

    private TriageResult latestTriageResult(Integer consultationId) {
        if (consultationId == null) return null;
        return triageResultMapper.selectOne(Wrappers.<TriageResult>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("is_final")
                .orderByDesc("id")
                .last("limit 1"));
    }

    private boolean canAutoRoute(ConsultationRecord record) {
        String status = trimToNull(record == null ? null : record.getStatus());
        return status == null
                || "submitted".equalsIgnoreCase(status)
                || "triaged".equalsIgnoreCase(status);
    }

    private boolean isGeneralEntryDepartment(ConsultationRecord record) {
        String departmentName = trimToNull(record == null ? null : record.getDepartmentName());
        if (departmentName != null && (departmentName.contains("全科") || departmentName.contains("综合"))) {
            return true;
        }

        Integer departmentId = record == null ? null : record.getDepartmentId();
        if (departmentId == null) return true;

        Department department = departmentMapper.selectById(departmentId);
        String departmentCode = trimToNull(department == null ? null : department.getCode());
        return departmentCode != null && "GENERAL_MEDICINE".equalsIgnoreCase(departmentCode);
    }

    private boolean isGeneralEntryDepartment(Department department) {
        if (department == null) return false;
        String departmentName = trimToNull(department.getName());
        if (departmentName != null && (departmentName.contains("全科") || departmentName.contains("综合"))) {
            return true;
        }
        String departmentCode = trimToNull(department.getCode());
        return departmentCode != null && "GENERAL_MEDICINE".equalsIgnoreCase(departmentCode);
    }

    private boolean isClaimed(Integer consultationId) {
        if (consultationId == null) return false;
        return consultationDoctorAssignmentMapper.exists(Wrappers.<ConsultationDoctorAssignment>query()
                .eq("consultation_id", consultationId)
                .eq("status", "claimed"));
    }

    private Department matchDepartment(String recommendedDepartmentName) {
        String normalizedTarget = normalizeDepartmentText(recommendedDepartmentName);
        if (normalizedTarget == null) return null;

        List<Department> departments = loadEnabledDepartments();
        if (departments.isEmpty()) return null;

        for (Department item : departments) {
            if (normalizedTarget.equals(normalizeDepartmentText(item.getName()))
                    || normalizedTarget.equals(normalizeDepartmentText(item.getCode()))) {
                return item;
            }
        }

        Department matched = null;
        int bestScore = -1;
        for (Department item : departments) {
            int score = matchScore(normalizedTarget, item);
            if (score > bestScore) {
                bestScore = score;
                matched = item;
            }
        }
        return bestScore > 0 ? matched : null;
    }

    private List<Department> loadEnabledDepartments() {
        return departmentMapper.selectList(Wrappers.<Department>query()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    private String buildDepartmentOptionsText(List<Department> departments) {
        if (departments == null || departments.isEmpty()) return null;
        return departments.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    StringBuilder builder = new StringBuilder("- ").append(safeText(item.getName(), "未命名科室"));
                    if (StringUtils.hasText(item.getCode())) {
                        builder.append("（").append(item.getCode().trim()).append("）");
                    }
                    String description = trimToNull(item.getDescription());
                    if (description != null) {
                        builder.append("：").append(abbreviate(description, 48));
                    }
                    if (isGeneralEntryDepartment(item)) {
                        builder.append(" [综合入口/兜底]");
                    }
                    return builder.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private int matchScore(String normalizedTarget, Department department) {
        String normalizedName = normalizeDepartmentText(department.getName());
        String normalizedCode = normalizeDepartmentText(department.getCode());
        int score = 0;
        if (normalizedName != null && (normalizedTarget.contains(normalizedName) || normalizedName.contains(normalizedTarget))) {
            score = Math.max(score, Math.min(normalizedTarget.length(), normalizedName.length()) * 10);
        }
        if (normalizedCode != null && (normalizedTarget.contains(normalizedCode) || normalizedCode.contains(normalizedTarget))) {
            score = Math.max(score, Math.min(normalizedTarget.length(), normalizedCode.length()) * 6);
        }
        return score;
    }

    private String normalizeDepartmentText(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        return text
                .replace("门诊", "")
                .replace("科室", "")
                .replace("专科", "")
                .replace("推荐", "")
                .replace("建议", "")
                .replace("就诊", "")
                .replace("挂号", "")
                .replace("方向", "")
                .replace("：", "")
                .replace(":", "")
                .replace(" ", "")
                .trim()
                .toUpperCase();
    }

    private String buildSessionSummary(ConsultationRecord record, AiTriageAdvice advice) {
        List<String> segments = new ArrayList<>();
        segments.add("分诊等级：" + safeText(record == null ? null : record.getTriageLevelName(), "待系统评估"));
        segments.add("建议动作：" + visitTypeLabel(record == null ? null : record.getTriageActionType()));
        if (trimToNull(record == null ? null : record.getDepartmentName()) != null) {
            segments.add("匹配科室：" + record.getDepartmentName());
        }
        if (trimToNull(advice == null ? null : advice.getSummary()) != null) {
            segments.add("AI摘要：" + abbreviate(advice.getSummary(), 160));
        } else if (trimToNull(record == null ? null : record.getTriageSuggestion()) != null) {
            segments.add("系统建议：" + abbreviate(record.getTriageSuggestion(), 160));
        }
        return abbreviate(String.join("；", segments), 1000);
    }

    private String mergeReasonText(String currentReasonText,
                                   AiTriageAdvice advice,
                                   Department department,
                                   ConsultationRecommendDoctorVO firstDoctor) {
        Set<String> segments = new LinkedHashSet<>();
        if (trimToNull(currentReasonText) != null) segments.add(trimToNull(currentReasonText));
        if (department != null) segments.add("AI建议科室：" + department.getName());
        if (trimToNull(advice == null ? null : advice.getSummary()) != null) {
            segments.add("AI导诊摘要：" + abbreviate(advice.getSummary(), 120));
        }
        if (trimToNull(advice == null ? null : advice.getDoctorRecommendationReason()) != null) {
            segments.add("AI推荐依据：" + abbreviate(advice.getDoctorRecommendationReason(), 120));
        }
        if (firstDoctor != null && trimToNull(firstDoctor.getRecommendationSummary()) != null) {
            segments.add("首推医生：" + firstDoctor.getName() + "，" + abbreviate(firstDoctor.getRecommendationSummary(), 120));
        }
        return segments.isEmpty() ? null : abbreviate(String.join("；", segments), 500);
    }

    private String visitTypeLabel(String visitType) {
        return switch (trimToNull(visitType) == null ? "" : visitType.trim().toLowerCase()) {
            case "emergency" -> "立即急诊";
            case "offline" -> "尽快线下就医";
            case "followup" -> "复诊随访";
            case "online" -> "线上继续沟通";
            default -> safeText(visitType, "线上继续沟通");
        };
    }

    private String abbreviate(String value, int maxLength) {
        String text = trimToNull(value);
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String safeText(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
