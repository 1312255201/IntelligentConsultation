package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.BodyPartDict;
import cn.gugufish.entity.dto.ConsultationCategory;
import cn.gugufish.entity.dto.ConsultationIntakeField;
import cn.gugufish.entity.dto.ConsultationIntakeTemplate;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.dto.PatientMedicalHistory;
import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.dto.RedFlagRule;
import cn.gugufish.entity.dto.RedFlagRuleSymptom;
import cn.gugufish.entity.dto.SymptomDict;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.dto.TriageFeedback;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.dto.TriageResult;
import cn.gugufish.entity.dto.TriageSession;
import cn.gugufish.entity.dto.TriageLevelDict;
import cn.gugufish.entity.vo.request.ConsultationAnswerSubmitVO;
import cn.gugufish.entity.vo.request.ConsultationRecordCreateVO;
import cn.gugufish.entity.vo.request.ConsultationTriageFeedbackSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackDoctorOptionVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackDepartmentOptionVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackOptionsVO;
import cn.gugufish.entity.vo.response.ConsultationEntryCategoryVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeFieldVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.ConsultationRecordVO;
import cn.gugufish.mapper.BodyPartDictMapper;
import cn.gugufish.mapper.ConsultationCategoryMapper;
import cn.gugufish.mapper.ConsultationIntakeFieldMapper;
import cn.gugufish.mapper.ConsultationIntakeTemplateMapper;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.mapper.PatientMedicalHistoryMapper;
import cn.gugufish.mapper.PatientProfileMapper;
import cn.gugufish.mapper.RedFlagRuleMapper;
import cn.gugufish.mapper.RedFlagRuleSymptomMapper;
import cn.gugufish.mapper.SymptomDictMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.mapper.TriageResultMapper;
import cn.gugufish.mapper.TriageMessageMapper;
import cn.gugufish.mapper.TriageSessionMapper;
import cn.gugufish.mapper.TriageLevelDictMapper;
import cn.gugufish.service.ConsultationService;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import cn.gugufish.service.ConsultationAiEnrichmentService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageFeedbackService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
public class ConsultationServiceImpl implements ConsultationService {

    private static final Set<String> SINGLE_PREFERRED_CODES = Set.of(
            "chief_complaint",
            "main_issue",
            "skin_problem",
            "disease_name",
            "abnormal_focus"
    );

    private static final SimpleDateFormat SCHEDULE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Resource ConsultationCategoryMapper consultationCategoryMapper;
    @Resource ConsultationIntakeTemplateMapper consultationIntakeTemplateMapper;
    @Resource ConsultationIntakeFieldMapper consultationIntakeFieldMapper;
    @Resource ConsultationRecordMapper consultationRecordMapper;
    @Resource ConsultationRecordAnswerMapper consultationRecordAnswerMapper;
    @Resource DepartmentMapper departmentMapper;
    @Resource PatientProfileMapper patientProfileMapper;
    @Resource PatientMedicalHistoryMapper patientMedicalHistoryMapper;
    @Resource RedFlagRuleMapper redFlagRuleMapper;
    @Resource RedFlagRuleSymptomMapper redFlagRuleSymptomMapper;
    @Resource TriageLevelDictMapper triageLevelDictMapper;
    @Resource SymptomDictMapper symptomDictMapper;
    @Resource BodyPartDictMapper bodyPartDictMapper;
    @Resource DoctorMapper doctorMapper;
    @Resource DoctorScheduleMapper doctorScheduleMapper;
    @Resource DoctorServiceTagMapper doctorServiceTagMapper;
    @Resource TriageRuleHitLogMapper triageRuleHitLogMapper;
    @Resource TriageSessionMapper triageSessionMapper;
    @Resource TriageMessageMapper triageMessageMapper;
    @Resource TriageResultMapper triageResultMapper;
    @Resource TriageSessionQueryService triageSessionQueryService;
    @Resource TriageResultQueryService triageResultQueryService;
    @Resource TriageFeedbackQueryService triageFeedbackQueryService;
    @Resource ConsultationDoctorConclusionQueryService consultationDoctorConclusionQueryService;
    @Resource ConsultationDoctorFollowUpQueryService consultationDoctorFollowUpQueryService;
    @Resource ConsultationDoctorHandleQueryService consultationDoctorHandleQueryService;
    @Resource ConsultationAiEnrichmentService consultationAiEnrichmentService;
    @Resource TriageFeedbackService triageFeedbackService;

    @Override
    public List<ConsultationEntryCategoryVO> listEntryCategories() {
        List<ConsultationCategory> categories = consultationCategoryMapper.selectList(Wrappers.<ConsultationCategory>query()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        if (categories.isEmpty()) return List.of();

        List<Integer> categoryIds = categories.stream().map(ConsultationCategory::getId).toList();
        List<ConsultationIntakeTemplate> templates = consultationIntakeTemplateMapper.selectList(Wrappers.<ConsultationIntakeTemplate>query()
                .in("category_id", categoryIds)
                .eq("status", 1)
                .orderByAsc("category_id")
                .orderByDesc("is_default")
                .orderByDesc("version")
                .orderByDesc("id"));
        Map<Integer, ConsultationIntakeTemplate> templateMap = new HashMap<>();
        templates.forEach(item -> templateMap.putIfAbsent(item.getCategoryId(), item));

        List<Integer> templateIds = templateMap.values().stream().map(ConsultationIntakeTemplate::getId).toList();
        Map<Integer, Integer> fieldCountMap = templateIds.isEmpty()
                ? Map.of()
                : consultationIntakeFieldMapper.selectList(Wrappers.<ConsultationIntakeField>query()
                .in("template_id", templateIds)
                .eq("status", 1)).stream()
                .collect(Collectors.groupingBy(ConsultationIntakeField::getTemplateId, Collectors.summingInt(item -> 1)));

        Map<Integer, String> departmentMap = departmentMapper.selectBatchIds(categories.stream()
                        .map(ConsultationCategory::getDepartmentId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        return categories.stream()
                .filter(item -> templateMap.containsKey(item.getId()))
                .map(item -> {
                    ConsultationIntakeTemplate template = templateMap.get(item.getId());
                    ConsultationEntryCategoryVO vo = new ConsultationEntryCategoryVO();
                    vo.setId(item.getId());
                    vo.setDepartmentId(item.getDepartmentId());
                    vo.setDepartmentName(departmentMap.get(item.getDepartmentId()));
                    vo.setName(item.getName());
                    vo.setCode(item.getCode());
                    vo.setDescription(item.getDescription());
                    vo.setSort(item.getSort());
                    vo.setDefaultTemplateId(template.getId());
                    vo.setDefaultTemplateName(template.getName());
                    vo.setDefaultTemplateDescription(template.getDescription());
                    vo.setDefaultTemplateFieldCount(fieldCountMap.getOrDefault(template.getId(), 0));
                    return vo;
                })
                .toList();
    }

    @Override
    public ConsultationIntakeTemplateVO defaultTemplateDetail(int categoryId) {
        ConsultationCategory category = consultationCategoryMapper.selectOne(Wrappers.<ConsultationCategory>query()
                .eq("id", categoryId)
                .eq("status", 1));
        if (category == null) return null;

        ConsultationIntakeTemplate template = pickDefaultTemplate(categoryId);
        if (template == null) return null;

        List<ConsultationIntakeFieldVO> fields = consultationIntakeFieldMapper.selectList(Wrappers.<ConsultationIntakeField>query()
                        .eq("template_id", template.getId())
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationIntakeFieldVO.class))
                .toList();

        return template.asViewObject(ConsultationIntakeTemplateVO.class, vo -> {
            vo.setFieldCount(fields.size());
            vo.setFields(fields);
        });
    }

    @Override
    public List<ConsultationRecordVO> listRecords(int accountId) {
        return consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                        .eq("account_id", accountId)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationRecordVO.class))
                .toList();
    }

    @Override
    public ConsultationRecordVO recordDetail(int accountId, int id) {
        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", id)
                .eq("account_id", accountId));
        if (record == null) return null;

        List<ConsultationRecordAnswerVO> answers = consultationRecordAnswerMapper.selectList(Wrappers.<ConsultationRecordAnswer>query()
                        .eq("consultation_id", id)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationRecordAnswerVO.class))
                .toList();

        return record.asViewObject(ConsultationRecordVO.class, vo -> {
            vo.setAnswers(answers);
            vo.setRecommendedDoctors(buildRecommendedDoctors(record));
            vo.setDoctorHandle(consultationDoctorHandleQueryService.detailByConsultationId(id));
            vo.setDoctorConclusion(consultationDoctorConclusionQueryService.detailByConsultationId(id));
            vo.setDoctorFollowUps(consultationDoctorFollowUpQueryService.listByConsultationId(id));
            vo.setTriageSession(triageSessionQueryService.detailByConsultationId(id));
            vo.setTriageResult(triageResultQueryService.detailByConsultationId(id));
            vo.setTriageFeedback(triageFeedbackQueryService.detailByConsultationId(id));
        });
    }

    @Override
    public ConsultationFeedbackOptionsVO feedbackOptions() {
        return triageFeedbackService.feedbackOptions();
    }

    @Override
    @Transactional
    public String createRecord(int accountId, ConsultationRecordCreateVO vo) {
        PatientProfile patient = patientProfileMapper.selectOne(Wrappers.<PatientProfile>query()
                .eq("id", vo.getPatientId())
                .eq("account_id", accountId)
                .eq("status", 1));
        if (patient == null) return "就诊人不存在或已停用";

        ConsultationCategory category = consultationCategoryMapper.selectOne(Wrappers.<ConsultationCategory>query()
                .eq("id", vo.getCategoryId())
                .eq("status", 1));
        if (category == null) return "问诊分类不存在或未启用";

        ConsultationIntakeTemplate template = consultationIntakeTemplateMapper.selectOne(Wrappers.<ConsultationIntakeTemplate>query()
                .eq("id", vo.getTemplateId())
                .eq("category_id", vo.getCategoryId())
                .eq("status", 1));
        if (template == null) return "问诊模板不存在或未启用";

        List<ConsultationIntakeField> fields = consultationIntakeFieldMapper.selectList(Wrappers.<ConsultationIntakeField>query()
                .eq("template_id", template.getId())
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        if (fields.isEmpty()) return "当前问诊模板暂无可用字段";

        Map<String, ConsultationIntakeField> fieldMap = fields.stream()
                .collect(Collectors.toMap(ConsultationIntakeField::getFieldCode, item -> item));
        Map<String, String> submittedMap = normalizeSubmittedAnswers(vo.getAnswers(), fieldMap);
        if (submittedMap == null) return "提交数据中存在无效字段或重复字段，请刷新后重试";

        Map<String, String> normalizedValues = new HashMap<>();
        for (ConsultationIntakeField field : fields) {
            String sourceValue = submittedMap.containsKey(field.getFieldCode()) ? submittedMap.get(field.getFieldCode()) : field.getDefaultValue();
            NormalizeResult result = normalizeFieldValue(field, sourceValue);
            if (result.message() != null) {
                return "字段“" + field.getFieldLabel() + "”" + result.message();
            }
            normalizedValues.put(field.getFieldCode(), result.value());
        }

        List<ConsultationRecordAnswer> answers = new ArrayList<>();
        boolean hasAnyValue = false;
        for (ConsultationIntakeField field : fields) {
            if (!isFieldVisible(field.getConditionRule(), patient, normalizedValues)) continue;
            String value = normalizedValues.get(field.getFieldCode());
            if (field.getIsRequired() != null && field.getIsRequired() == 1 && isEmptyValue(value)) {
                return "请完善必填字段“" + field.getFieldLabel() + "”";
            }
            if (shouldPersistValue(field.getFieldType(), value)) {
                answers.add(new ConsultationRecordAnswer(null, null, field.getFieldCode(), field.getFieldLabel(), field.getFieldType(), value, field.getSort(), null, null));
                hasAnyValue = true;
            }
        }
        if (!hasAnyValue) return "请至少完善一项问诊资料后再提交";

        PatientMedicalHistory history = patientMedicalHistoryMapper.selectOne(Wrappers.<PatientMedicalHistory>query().eq("patient_id", patient.getId()));
        String chiefComplaint = buildChiefComplaint(fields, normalizedValues);
        String healthSummary = buildHealthSummary(history);
        Department department = category.getDepartmentId() == null ? null : departmentMapper.selectById(category.getDepartmentId());
        String departmentName = department == null ? null : department.getName();
        TriageDecision triage = evaluateTriage(category, chiefComplaint, healthSummary, answers);
        Date now = new Date();
        ConsultationRecord record = new ConsultationRecord(
                null, generateConsultationNo(), accountId, patient.getId(), patient.getName(),
                category.getId(), category.getName(), category.getDepartmentId(), departmentName,
                template.getId(), template.getName(), buildTitle(patient.getName(), category.getName(), chiefComplaint),
                chiefComplaint, healthSummary, triage.levelCode() == null ? "submitted" : "triaged", answers.size(),
                triage.levelId(), triage.levelCode(), triage.levelName(), triage.levelColor(),
                triage.actionType(), triage.suggestion(), triage.ruleSummary(), now, now
        );
        if (consultationRecordMapper.insert(record) <= 0) return "问诊提交失败，请稍后重试";

        for (ConsultationRecordAnswer answer : answers) {
            answer.setConsultationId(record.getId());
            answer.setCreateTime(now);
            answer.setUpdateTime(now);
            if (consultationRecordAnswerMapper.insert(answer) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return "问诊答案保存失败，请稍后重试";
            }
        }

        String hitLogMessage = saveTriageRuleHitLogs(record.getId(), triage.ruleHits(), now);
        if (hitLogMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return hitLogMessage;
        }

        String sessionMessage = saveTriageSession(record, triage, answers, now);
        if (sessionMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return sessionMessage;
        }

        String resultMessage = saveTriageResult(record, triage, answers, now);
        if (resultMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return resultMessage;
        }
        triggerAiTriageEnrichment(record.getId());
        return null;
    }

    @Override
    public String submitTriageFeedback(int accountId, ConsultationTriageFeedbackSubmitVO vo) {
        return triageFeedbackService.submitFeedback(accountId, vo);
    }

    private ConsultationIntakeTemplate pickDefaultTemplate(int categoryId) {
        return consultationIntakeTemplateMapper.selectList(Wrappers.<ConsultationIntakeTemplate>query()
                        .eq("category_id", categoryId)
                        .eq("status", 1)
                        .orderByDesc("is_default")
                        .orderByDesc("version")
                        .orderByDesc("id"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> normalizeSubmittedAnswers(List<ConsultationAnswerSubmitVO> answers,
                                                          Map<String, ConsultationIntakeField> fieldMap) {
        Map<String, String> result = new HashMap<>();
        if (answers == null || answers.isEmpty()) return result;
        for (ConsultationAnswerSubmitVO answer : answers) {
            String fieldCode = trimToNull(answer.getFieldCode());
            if (fieldCode == null || !fieldMap.containsKey(fieldCode)) return null;
            if (result.containsKey(fieldCode)) return null;
            result.put(fieldCode, answer.getFieldValue());
        }
        return result;
    }

    private NormalizeResult normalizeFieldValue(ConsultationIntakeField field, String rawValue) {
        return switch (field.getFieldType()) {
            case "input", "textarea", "date", "upload" -> new NormalizeResult(trimToNull(rawValue), null);
            case "number" -> normalizeNumber(rawValue);
            case "switch" -> normalizeSwitch(rawValue);
            case "single_select" -> normalizeSingleSelect(field, rawValue);
            case "multi_select" -> normalizeMultiSelect(field, rawValue);
            default -> new NormalizeResult(null, "存在不支持的字段类型");
        };
    }

    private NormalizeResult normalizeNumber(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) return new NormalizeResult(null, null);
        return value.matches("^-?\\d+(\\.\\d+)?$")
                ? new NormalizeResult(value, null)
                : new NormalizeResult(null, "请输入有效数字");
    }

    private NormalizeResult normalizeSwitch(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) return new NormalizeResult(null, null);
        return switch (value.toLowerCase()) {
            case "1", "true", "yes", "on" -> new NormalizeResult("1", null);
            case "0", "false", "no", "off" -> new NormalizeResult("0", null);
            default -> new NormalizeResult(null, "请选择有效的开关值");
        };
    }

    private NormalizeResult normalizeSingleSelect(ConsultationIntakeField field, String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) return new NormalizeResult(null, null);
        List<String> options = parseOptions(field.getOptionsJson());
        if (!options.isEmpty() && !options.contains(value)) {
            return new NormalizeResult(null, "选择值不在可选范围内");
        }
        return new NormalizeResult(value, null);
    }

    private NormalizeResult normalizeMultiSelect(ConsultationIntakeField field, String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) return new NormalizeResult(null, null);
        try {
            List<String> values = JSON.parseArray(value, String.class).stream()
                    .map(this::trimToNull)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            if (values.isEmpty()) return new NormalizeResult(null, null);
            List<String> options = parseOptions(field.getOptionsJson());
            if (!options.isEmpty() && values.stream().anyMatch(item -> !options.contains(item))) {
                return new NormalizeResult(null, "存在不在可选范围内的值");
            }
            return new NormalizeResult(JSON.toJSONString(values), null);
        } catch (Exception exception) {
            return new NormalizeResult(null, "多选字段格式不正确");
        }
    }

    private boolean isFieldVisible(String conditionRule, PatientProfile patient, Map<String, String> answers) {
        String rule = trimToNull(conditionRule);
        if (rule == null) return true;
        if (rule.contains("||")) {
            return Arrays.stream(rule.split("\\|\\|"))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .anyMatch(item -> evaluateClause(item, patient, answers));
        }
        return Arrays.stream(rule.split("&&"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .allMatch(item -> evaluateClause(item, patient, answers));
    }

    private boolean evaluateClause(String clause, PatientProfile patient, Map<String, String> answers) {
        String operator = clause.contains("!=") ? "!=" : "=";
        int index = clause.indexOf(operator);
        if (index <= 0) return true;

        String key = clause.substring(0, index).trim();
        String expectedText = clause.substring(index + operator.length()).trim();
        List<String> expectedValues = Arrays.stream(expectedText.split("[|,]"))
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (expectedValues.isEmpty()) return true;

        List<String> currentValues = readCurrentValues(key, patient, answers);
        boolean matched = currentValues.stream().anyMatch(expectedValues::contains);
        return "=".equals(operator) ? matched : !matched;
    }

    private List<String> readCurrentValues(String key, PatientProfile patient, Map<String, String> answers) {
        String current = switch (key) {
            case "gender", "patientGender" -> patient.getGender();
            default -> answers.get(key);
        };
        if (isEmptyValue(current)) return List.of();
        try {
            return JSON.parseArray(current, String.class).stream()
                    .map(this::trimToNull)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception exception) {
            return List.of(current);
        }
    }

    private String buildChiefComplaint(List<ConsultationIntakeField> fields, Map<String, String> values) {
        for (ConsultationIntakeField field : fields) {
            if (SINGLE_PREFERRED_CODES.contains(field.getFieldCode()) && !isEmptyValue(values.get(field.getFieldCode()))) {
                return abbreviate(displayValue(field.getFieldType(), values.get(field.getFieldCode())), 500);
            }
        }
        for (ConsultationIntakeField field : fields) {
            String value = values.get(field.getFieldCode());
            if (!isEmptyValue(value)) {
                return abbreviate(displayValue(field.getFieldType(), value), 500);
            }
        }
        return null;
    }

    private String buildTitle(String patientName, String categoryName, String chiefComplaint) {
        if (!isEmptyValue(chiefComplaint)) return abbreviate(chiefComplaint, 100);
        return abbreviate(patientName + " - " + categoryName, 100);
    }

    private String buildHealthSummary(PatientMedicalHistory history) {
        if (history == null) return null;
        List<String> segments = new ArrayList<>();
        if (!isEmptyValue(history.getAllergyHistory())) segments.add("过敏史：" + abbreviate(history.getAllergyHistory(), 50));
        if (!isEmptyValue(history.getChronicHistory())) segments.add("慢病史：" + abbreviate(history.getChronicHistory(), 50));
        if (!isEmptyValue(history.getPastHistory())) segments.add("既往史：" + abbreviate(history.getPastHistory(), 50));
        if (!isEmptyValue(history.getMedicationHistory())) segments.add("长期用药：" + abbreviate(history.getMedicationHistory(), 50));
        if (!isEmptyValue(history.getPregnancyStatus()) && !"unknown".equals(history.getPregnancyStatus())) {
            segments.add("孕期状态：" + history.getPregnancyStatus());
        }
        if (!isEmptyValue(history.getLactationStatus()) && !"unknown".equals(history.getLactationStatus())) {
            segments.add("哺乳状态：" + history.getLactationStatus());
        }
        return segments.isEmpty() ? null : abbreviate(String.join("；", segments), 500);
    }

    private boolean shouldPersistValue(String fieldType, String value) {
        if (isEmptyValue(value)) return false;
        return !"multi_select".equals(fieldType) || !"[]".equals(value);
    }

    private String displayValue(String fieldType, String value) {
        if (isEmptyValue(value)) return "";
        return switch (fieldType) {
            case "multi_select" -> {
                try {
                    yield String.join("、", JSON.parseArray(value, String.class));
                } catch (Exception exception) {
                    yield value;
                }
            }
            case "switch" -> "1".equals(value) ? "是" : "否";
            case "upload" -> "已上传图片资料";
            default -> value;
        };
    }

    private List<String> parseOptions(String optionsJson) {
        String text = trimToNull(optionsJson);
        if (text == null) return List.of();
        try {
            return JSON.parseArray(text, String.class).stream()
                    .map(this::trimToNull)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private TriageDecision evaluateTriage(ConsultationCategory category,
                                          String chiefComplaint,
                                          String healthSummary,
                                          List<ConsultationRecordAnswer> answers) {
        List<TriageLevelDict> levels = triageLevelDictMapper.selectList(Wrappers.<TriageLevelDict>query()
                .eq("status", 1)
                .orderByDesc("priority")
                .orderByAsc("sort")
                .orderByAsc("id"));
        TriageLevelDict defaultLevel = pickDefaultTriageLevel(category, levels);
        if (levels.isEmpty()) {
            return new TriageDecision(null, null, null, null, null, "当前系统尚未配置分诊等级，问诊资料已先保存。", "当前系统尚未配置分诊等级", List.of());
        }

        String textCorpus = buildTriageTextCorpus(chiefComplaint, healthSummary, answers);
        List<RedFlagRule> rules = redFlagRuleMapper.selectList(Wrappers.<RedFlagRule>query()
                .eq("status", 1)
                .orderByDesc("priority")
                .orderByAsc("id"));
        if (rules.isEmpty()) return buildDefaultTriageDecision(defaultLevel, category);

        List<Integer> ruleIds = rules.stream().map(RedFlagRule::getId).toList();
        Map<Integer, List<Integer>> ruleSymptomIdsMap = redFlagRuleSymptomMapper.selectList(Wrappers.<RedFlagRuleSymptom>query()
                        .in("rule_id", ruleIds))
                .stream()
                .collect(Collectors.groupingBy(RedFlagRuleSymptom::getRuleId,
                        Collectors.mapping(RedFlagRuleSymptom::getSymptomId, Collectors.toList())));

        List<SymptomDict> symptoms = symptomDictMapper.selectList(Wrappers.<SymptomDict>query()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        List<BodyPartDict> bodyParts = bodyPartDictMapper.selectList(Wrappers.<BodyPartDict>query()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        Map<Integer, SymptomDict> symptomMap = symptoms.stream()
                .collect(Collectors.toMap(SymptomDict::getId, item -> item));
        Map<Integer, BodyPartDict> bodyPartMap = bodyParts.stream()
                .collect(Collectors.toMap(BodyPartDict::getId, item -> item));
        Map<Integer, TriageLevelDict> levelMap = levels.stream()
                .collect(Collectors.toMap(TriageLevelDict::getId, item -> item));

        Set<Integer> matchedSymptomIds = extractMatchedSymptomIds(textCorpus, symptoms);
        Set<Integer> matchedBodyPartIds = extractMatchedBodyPartIds(textCorpus, bodyParts, matchedSymptomIds, symptomMap, bodyPartMap);
        List<RuleMatch> matchedRules = rules.stream()
                .filter(rule -> matchesRule(rule, ruleSymptomIdsMap.getOrDefault(rule.getId(), List.of()), matchedSymptomIds, matchedBodyPartIds, textCorpus))
                .map(rule -> new RuleMatch(
                        rule,
                        levelMap.get(rule.getTriageLevelId()),
                        buildRuleMatchedSummary(rule, ruleSymptomIdsMap.getOrDefault(rule.getId(), List.of()), matchedSymptomIds, matchedBodyPartIds, symptomMap, bodyPartMap)))
                .toList();
        if (matchedRules.isEmpty()) return buildDefaultTriageDecision(defaultLevel, category);

        RuleMatch primaryMatch = matchedRules.stream()
                .max(Comparator
                        .comparingInt((RuleMatch item) -> item.level() == null ? Integer.MIN_VALUE : defaultInt(item.level().getPriority()))
                        .thenComparingInt(item -> defaultInt(item.rule().getPriority())))
                .orElse(null);
        if (primaryMatch == null) return buildDefaultTriageDecision(defaultLevel, category);

        TriageLevelDict level = primaryMatch.level() == null ? defaultLevel : primaryMatch.level();
        String suggestion = trimToNull(primaryMatch.rule().getSuggestion());
        if (suggestion == null) suggestion = defaultTriageSuggestion(level, category);
        String actionType = trimToNull(primaryMatch.rule().getActionType());
        if (actionType == null) actionType = defaultActionType(level);
        String ruleSummary = abbreviate(matchedRules.stream()
                .map(item -> item.rule().getRuleName())
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("；")), 500);
        List<TriageRuleHitSnapshot> ruleHits = matchedRules.stream()
                .map(item -> new TriageRuleHitSnapshot(
                        item.rule().getId(),
                        item.rule().getRuleName(),
                        item.rule().getRuleCode(),
                        item.rule().getTriggerType(),
                        item.level() == null ? null : item.level().getId(),
                        item.level() == null ? null : item.level().getCode(),
                        item.level() == null ? null : item.level().getName(),
                        trimToNull(item.rule().getActionType()),
                        trimToNull(item.rule().getSuggestion()),
                        item.matchedSummary(),
                        item.rule().getPriority(),
                        primaryMatch.rule().getId().equals(item.rule().getId()) ? 1 : 0
                ))
                .toList();
        return new TriageDecision(
                level == null ? null : level.getId(),
                level == null ? null : level.getCode(),
                level == null ? null : level.getName(),
                level == null ? null : level.getColor(),
                actionType,
                suggestion,
                ruleSummary,
                ruleHits
        );
    }

    private TriageDecision buildDefaultTriageDecision(TriageLevelDict level, ConsultationCategory category) {
        if (level == null) {
            return new TriageDecision(null, null, null, null, null, "系统尚未完成默认分诊配置，问诊资料已先保存。", "系统未配置默认分诊策略", List.of());
        }
        return new TriageDecision(
                level.getId(),
                level.getCode(),
                level.getName(),
                level.getColor(),
                defaultActionType(level),
                defaultTriageSuggestion(level, category),
                "未命中红旗规则，已按问诊分类使用默认分诊策略",
                List.of()
        );
    }

    private TriageLevelDict pickDefaultTriageLevel(ConsultationCategory category, List<TriageLevelDict> levels) {
        if (levels.isEmpty()) return null;
        String categoryCode = trimToNull(category.getCode());
        String categoryName = trimToNull(category.getName());
        boolean preferFollowup = (categoryCode != null && categoryCode.toUpperCase().contains("FOLLOWUP"))
                || (categoryName != null && categoryName.contains("复诊"));
        if (preferFollowup) {
            TriageLevelDict followup = findLevelByCode(levels, "FOLLOWUP");
            if (followup != null) return followup;
        }
        TriageLevelDict normal = findLevelByCode(levels, "NORMAL");
        if (normal != null) return normal;
        TriageLevelDict followup = findLevelByCode(levels, "FOLLOWUP");
        if (followup != null) return followup;
        return levels.stream()
                .min(Comparator
                        .comparingInt((TriageLevelDict item) -> defaultInt(item.getPriority()))
                        .thenComparingInt(item -> defaultInt(item.getSort()))
                        .thenComparingInt(TriageLevelDict::getId))
                .orElse(levels.get(levels.size() - 1));
    }

    private TriageLevelDict findLevelByCode(List<TriageLevelDict> levels, String code) {
        return levels.stream()
                .filter(item -> code.equalsIgnoreCase(item.getCode()))
                .findFirst()
                .orElse(null);
    }

    private String defaultActionType(TriageLevelDict level) {
        if (level == null) return "online";
        return switch (trimToNull(level.getCode()) == null ? "" : level.getCode().toUpperCase()) {
            case "EMERGENCY" -> "emergency";
            case "URGENT" -> "offline";
            case "FOLLOWUP" -> "followup";
            default -> "online";
        };
    }

    private String defaultTriageSuggestion(TriageLevelDict level, ConsultationCategory category) {
        String suggestion = level == null ? null : trimToNull(level.getSuggestion());
        if (suggestion != null) return suggestion;
        String categoryName = category == null ? "当前问诊" : category.getName();
        if (level == null || trimToNull(level.getCode()) == null) {
            return "系统已保存“" + categoryName + "”资料，请稍后查看后续处理结果。";
        }
        return switch (level.getCode().toUpperCase()) {
            case "EMERGENCY" -> "系统初步识别为高风险场景，请立即前往急诊或联系急救，不建议继续等待线上回复。";
            case "URGENT" -> "系统建议尽快线下就医，并结合症状变化持续观察，如出现加重请立即处理。";
            case "FOLLOWUP" -> "当前更偏向复诊/随访场景，建议继续补充资料并优先联系对应医生。";
            default -> "当前资料适合进入“" + categoryName + "”流程，系统已按分类匹配对应科室并生成初步建议。";
        };
    }

    private String buildTriageTextCorpus(String chiefComplaint, String healthSummary, List<ConsultationRecordAnswer> answers) {
        List<String> segments = new ArrayList<>();
        if (!isEmptyValue(chiefComplaint)) segments.add(chiefComplaint);
        if (!isEmptyValue(healthSummary)) segments.add(healthSummary);
        for (ConsultationRecordAnswer answer : answers) {
            if ("upload".equals(answer.getFieldType())) continue;
            String value = displayValue(answer.getFieldType(), answer.getFieldValue());
            if (!isEmptyValue(value)) segments.add(answer.getFieldLabel() + " " + value);
        }
        return String.join(" ", segments).toLowerCase();
    }

    private Set<Integer> extractMatchedSymptomIds(String textCorpus, List<SymptomDict> symptoms) {
        Set<Integer> matched = new HashSet<>();
        if (isEmptyValue(textCorpus) || symptoms.isEmpty()) return matched;
        for (SymptomDict symptom : symptoms) {
            if (matchesSymptom(textCorpus, symptom)) matched.add(symptom.getId());
        }
        return matched;
    }

    private Set<Integer> extractMatchedBodyPartIds(String textCorpus,
                                                   List<BodyPartDict> bodyParts,
                                                   Set<Integer> matchedSymptomIds,
                                                   Map<Integer, SymptomDict> symptomMap,
                                                   Map<Integer, BodyPartDict> bodyPartMap) {
        Set<Integer> matched = new HashSet<>();
        for (BodyPartDict bodyPart : bodyParts) {
            if (containsToken(textCorpus, bodyPart.getName())) addBodyPartWithParents(matched, bodyPart.getId(), bodyPartMap);
        }
        for (Integer symptomId : matchedSymptomIds) {
            SymptomDict symptom = symptomMap.get(symptomId);
            if (symptom != null && symptom.getBodyPartId() != null) {
                addBodyPartWithParents(matched, symptom.getBodyPartId(), bodyPartMap);
            }
        }
        return matched;
    }

    private void addBodyPartWithParents(Set<Integer> matched, Integer bodyPartId, Map<Integer, BodyPartDict> bodyPartMap) {
        Integer currentId = bodyPartId;
        while (currentId != null && matched.add(currentId)) {
            BodyPartDict current = bodyPartMap.get(currentId);
            currentId = current == null ? null : current.getParentId();
        }
    }

    private boolean matchesSymptom(String textCorpus, SymptomDict symptom) {
        List<String> tokens = new ArrayList<>();
        tokens.add(symptom.getName());
        tokens.addAll(splitKeywordTokens(symptom.getKeywords()));
        tokens.addAll(splitKeywordTokens(symptom.getAliasKeywords()));
        return tokens.stream()
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .anyMatch(token -> containsToken(textCorpus, token));
    }

    private boolean matchesRule(RedFlagRule rule,
                                List<Integer> ruleSymptomIds,
                                Set<Integer> matchedSymptomIds,
                                Set<Integer> matchedBodyPartIds,
                                String textCorpus) {
        boolean anySymptomMatched = ruleSymptomIds.stream().anyMatch(matchedSymptomIds::contains);
        boolean allSymptomMatched = ruleSymptomIds.isEmpty() || matchedSymptomIds.containsAll(ruleSymptomIds);
        boolean bodyPartMatched = rule.getBodyPartId() != null && matchedBodyPartIds.contains(rule.getBodyPartId());
        boolean keywordMatched = matchesKeywordPattern(textCorpus, rule.getKeywordPattern());
        return switch (rule.getTriggerType()) {
            case "symptom_match" -> anySymptomMatched;
            case "body_part_match" -> bodyPartMatched;
            case "keyword_match" -> keywordMatched;
            case "combination" -> {
                boolean hasCondition = false;
                boolean matched = true;
                if (!ruleSymptomIds.isEmpty()) {
                    hasCondition = true;
                    matched = matched && allSymptomMatched;
                }
                if (rule.getBodyPartId() != null) {
                    hasCondition = true;
                    matched = matched && bodyPartMatched;
                }
                if (!isEmptyValue(rule.getKeywordPattern())) {
                    hasCondition = true;
                    matched = matched && keywordMatched;
                }
                yield hasCondition && matched;
            }
            default -> false;
        };
    }

    private boolean matchesKeywordPattern(String textCorpus, String keywordPattern) {
        String patternText = trimToNull(keywordPattern);
        if (patternText == null || isEmptyValue(textCorpus)) return false;
        try {
            return Pattern.compile(patternText, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(textCorpus)
                    .find();
        } catch (PatternSyntaxException exception) {
            return splitKeywordTokens(patternText).stream().anyMatch(token -> containsToken(textCorpus, token));
        }
    }

    private List<String> splitKeywordTokens(String text) {
        String value = trimToNull(text);
        if (value == null) return List.of();
        return Arrays.stream(value.split("[|,，、;；/\\n\\r]+"))
                .map(this::trimToNull)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private boolean containsToken(String textCorpus, String token) {
        String source = trimToNull(textCorpus);
        String currentToken = trimToNull(token);
        if (source == null || currentToken == null) return false;
        if (currentToken.length() == 1 && !currentToken.matches("\\d")) return false;
        return source.toLowerCase().contains(currentToken.toLowerCase());
    }

    private String buildRuleMatchedSummary(RedFlagRule rule,
                                           List<Integer> ruleSymptomIds,
                                           Set<Integer> matchedSymptomIds,
                                           Set<Integer> matchedBodyPartIds,
                                           Map<Integer, SymptomDict> symptomMap,
                                           Map<Integer, BodyPartDict> bodyPartMap) {
        List<String> segments = new ArrayList<>();
        if (!ruleSymptomIds.isEmpty()) {
            String symptomSummary = ruleSymptomIds.stream()
                    .filter(matchedSymptomIds::contains)
                    .map(symptomMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(SymptomDict::getName)
                    .distinct()
                    .collect(Collectors.joining("、"));
            if (!isEmptyValue(symptomSummary)) segments.add("命中症状：" + symptomSummary);
        }
        if (rule.getBodyPartId() != null && matchedBodyPartIds.contains(rule.getBodyPartId())) {
            BodyPartDict bodyPart = bodyPartMap.get(rule.getBodyPartId());
            if (bodyPart != null) segments.add("命中部位：" + bodyPart.getName());
        }
        if (!isEmptyValue(rule.getKeywordPattern())) {
            segments.add("命中文本模式：" + abbreviate(rule.getKeywordPattern(), 80));
        }
        if (segments.isEmpty() && !isEmptyValue(rule.getConditionDescription())) {
            segments.add(abbreviate(rule.getConditionDescription(), 120));
        }
        return segments.isEmpty() ? null : abbreviate(String.join("；", segments), 255);
    }

    private String saveTriageSession(ConsultationRecord record,
                                     TriageDecision triage,
                                     List<ConsultationRecordAnswer> answers,
                                     Date now) {
        List<TriageMessageDraft> messageDrafts = buildTriageMessages(record, triage, answers);
        TriageSession session = new TriageSession(
                null,
                generateTriageSessionNo(),
                record.getId(),
                record.getAccountId(),
                record.getPatientId(),
                record.getPatientName(),
                record.getCategoryId(),
                record.getCategoryName(),
                record.getDepartmentId(),
                record.getDepartmentName(),
                "consultation_submit",
                "completed",
                record.getTriageLevelId(),
                record.getTriageLevelCode(),
                record.getTriageLevelName(),
                record.getTriageActionType(),
                buildTriageSessionSummary(record),
                messageDrafts.size(),
                now,
                now,
                now,
                now
        );
        if (triageSessionMapper.insert(session) <= 0) {
            return "Triage session save failed, please try again later";
        }

        for (int i = 0; i < messageDrafts.size(); i++) {
            TriageMessageDraft draft = messageDrafts.get(i);
            TriageMessage message = new TriageMessage(
                    null,
                    session.getId(),
                    draft.roleType(),
                    draft.messageType(),
                    draft.title(),
                    draft.content(),
                    draft.structuredContent(),
                    (i + 1) * 10,
                    now
            );
            if (triageMessageMapper.insert(message) <= 0) {
                return "Triage session message save failed, please try again later";
            }
        }
        return null;
    }

    private String saveTriageResult(ConsultationRecord record,
                                    TriageDecision triage,
                                    List<ConsultationRecordAnswer> answers,
                                    Date now) {
        TriageSession session = triageSessionMapper.selectOne(Wrappers.<TriageSession>query()
                .eq("consultation_id", record.getId()));
        if (session == null) return "导诊结果保存失败，请稍后重试";

        List<ConsultationRecommendDoctorVO> candidates = buildRecommendedDoctors(record);
        ConsultationRecommendDoctorVO firstDoctor = candidates.isEmpty() ? null : candidates.get(0);
        TriageResult result = new TriageResult(
                null,
                session.getId(),
                record.getId(),
                "initial",
                record.getTriageLevelId(),
                record.getTriageLevelCode(),
                record.getTriageLevelName(),
                record.getDepartmentId(),
                record.getDepartmentName(),
                firstDoctor == null ? null : firstDoctor.getId(),
                firstDoctor == null ? null : firstDoctor.getName(),
                JSON.toJSONString(candidates),
                buildTriageResultReason(record),
                buildRiskFlagsJson(record, triage.ruleHits()),
                buildSymptomExtractJson(answers),
                resolveConfidenceScore(triage),
                1,
                1,
                now,
                now
        );
        return triageResultMapper.insert(result) > 0 ? null : "导诊结果保存失败，请稍后重试";
    }

    private void triggerAiTriageEnrichment(Integer consultationId) {
        if (consultationId == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        consultationAiEnrichmentService.enrichInitialTriage(consultationId);
                    } catch (Exception ignored) {
                    }
                }
            });
            return;
        }
        try {
            consultationAiEnrichmentService.enrichInitialTriage(consultationId);
        } catch (Exception ignored) {
        }
    }

    private String buildTriageResultReason(ConsultationRecord record) {
        List<String> segments = new ArrayList<>();
        if (!isEmptyValue(record.getTriageSuggestion())) segments.add(record.getTriageSuggestion());
        if (!isEmptyValue(record.getTriageRuleSummary())) segments.add(record.getTriageRuleSummary());
        if (!isEmptyValue(record.getChiefComplaint())) segments.add("主诉摘要：" + abbreviate(record.getChiefComplaint(), 120));
        return segments.isEmpty() ? null : abbreviate(String.join("；", segments), 500);
    }

    private String buildRiskFlagsJson(ConsultationRecord record, List<TriageRuleHitSnapshot> ruleHits) {
        List<String> values = new ArrayList<>();
        if (ruleHits != null) {
            ruleHits.stream()
                    .map(TriageRuleHitSnapshot::ruleName)
                    .filter(value -> !isEmptyValue(value))
                    .distinct()
                    .forEach(values::add);
        }
        if (values.isEmpty() && !isEmptyValue(record.getTriageRuleSummary())) {
            values.add(record.getTriageRuleSummary());
        }
        return values.isEmpty() ? null : JSON.toJSONString(values);
    }

    private String buildSymptomExtractJson(List<ConsultationRecordAnswer> answers) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (ConsultationRecordAnswer answer : answers) {
            if ("upload".equals(answer.getFieldType())) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("fieldCode", answer.getFieldCode());
            item.put("fieldLabel", answer.getFieldLabel());
            item.put("displayValue", displayValue(answer.getFieldType(), answer.getFieldValue()));
            payload.add(item);
        }
        return payload.isEmpty() ? null : JSON.toJSONString(payload);
    }

    private BigDecimal resolveConfidenceScore(TriageDecision triage) {
        int hitCount = triage.ruleHits() == null ? 0 : triage.ruleHits().size();
        double base = switch (trimToNull(triage.levelCode()) == null ? "" : triage.levelCode().toUpperCase()) {
            case "EMERGENCY" -> 0.96;
            case "URGENT" -> 0.88;
            case "FOLLOWUP" -> 0.78;
            default -> 0.72;
        };
        double score = Math.min(base + hitCount * 0.03, 0.99);
        if (triage.levelCode() == null) score = 0.60;
        return BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private List<TriageMessageDraft> buildTriageMessages(ConsultationRecord record,
                                                         TriageDecision triage,
                                                         List<ConsultationRecordAnswer> answers) {
        List<TriageMessageDraft> drafts = new ArrayList<>();
        drafts.add(new TriageMessageDraft(
                "user",
                "intake_summary",
                "\u7528\u6237\u95ee\u8bca\u6458\u8981",
                buildIntakeSummaryContent(record, answers),
                buildIntakeSummaryStructuredContent(record, answers)
        ));
        drafts.add(new TriageMessageDraft(
                "user",
                "health_summary",
                "\u5065\u5eb7\u6863\u6848\u6458\u8981",
                isEmptyValue(record.getHealthSummary()) ? "\u5f53\u524d\u672a\u5173\u8054\u5065\u5eb7\u6863\u6848\u6458\u8981\u4fe1\u606f\u3002" : record.getHealthSummary(),
                buildHealthSummaryStructuredContent(record)
        ));
        drafts.add(new TriageMessageDraft(
                "system",
                "triage_result",
                "\u7cfb\u7edf\u5206\u8bca\u7ed3\u679c",
                buildTriageSessionSummary(record),
                buildTriageResultStructuredContent(record)
        ));
        if (triage.ruleHits() != null && !triage.ruleHits().isEmpty()) {
            drafts.add(new TriageMessageDraft(
                    "rule_engine",
                    "rule_summary",
                    "\u89c4\u5219\u547d\u4e2d\u6458\u8981",
                    buildRuleSummaryContent(record, triage.ruleHits()),
                    buildRuleSummaryStructuredContent(triage.ruleHits())
            ));
            for (TriageRuleHitSnapshot item : triage.ruleHits()) {
                drafts.add(new TriageMessageDraft(
                        "rule_engine",
                        "rule_hit",
                        isEmptyValue(item.ruleName()) ? "\u89c4\u5219\u547d\u4e2d\u8be6\u60c5" : item.ruleName(),
                        buildRuleHitContent(item),
                        buildRuleHitStructuredContent(item)
                ));
            }
        }
        return drafts;
    }

    private String buildIntakeSummaryContent(ConsultationRecord record, List<ConsultationRecordAnswer> answers) {
        List<String> segments = new ArrayList<>();
        segments.add("\u5c31\u8bca\u4eba\uff1a" + record.getPatientName());
        segments.add("\u95ee\u8bca\u5206\u7c7b\uff1a" + record.getCategoryName());
        if (!isEmptyValue(record.getChiefComplaint())) segments.add("\u4e3b\u8bc9\uff1a" + record.getChiefComplaint());
        String answerSummary = answers.stream()
                .filter(item -> !isEmptyValue(item.getFieldValue()))
                .map(item -> item.getFieldLabel() + "\uff1a" + abbreviate(displayValue(item.getFieldType(), item.getFieldValue()), 60))
                .limit(6)
                .collect(Collectors.joining("\uff1b"));
        if (!isEmptyValue(answerSummary)) segments.add("\u8865\u5145\u8d44\u6599\uff1a" + answerSummary);
        return abbreviate(String.join("\uff1b", segments), 1000);
    }

    private String buildIntakeSummaryStructuredContent(ConsultationRecord record, List<ConsultationRecordAnswer> answers) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("consultationNo", record.getConsultationNo());
        payload.put("patientName", record.getPatientName());
        payload.put("categoryName", record.getCategoryName());
        payload.put("departmentName", record.getDepartmentName());
        payload.put("chiefComplaint", record.getChiefComplaint());

        List<Map<String, Object>> answerPayload = new ArrayList<>();
        for (ConsultationRecordAnswer answer : answers) {
            Map<String, Object> item = new HashMap<>();
            item.put("fieldCode", answer.getFieldCode());
            item.put("fieldLabel", answer.getFieldLabel());
            item.put("fieldType", answer.getFieldType());
            item.put("fieldValue", answer.getFieldValue());
            item.put("displayValue", displayValue(answer.getFieldType(), answer.getFieldValue()));
            answerPayload.add(item);
        }
        payload.put("answers", answerPayload);
        return JSON.toJSONString(payload);
    }

    private String buildHealthSummaryStructuredContent(ConsultationRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("patientName", record.getPatientName());
        payload.put("healthSummary", record.getHealthSummary());
        return JSON.toJSONString(payload);
    }

    private String buildTriageSessionSummary(ConsultationRecord record) {
        List<String> segments = new ArrayList<>();
        segments.add("\u5206\u8bca\u7b49\u7ea7\uff1a" + (isEmptyValue(record.getTriageLevelName()) ? "\u5f85\u7cfb\u7edf\u8bc4\u4f30" : record.getTriageLevelName()));
        segments.add("\u5efa\u8bae\u52a8\u4f5c\uff1a" + triageActionText(record.getTriageActionType()));
        if (!isEmptyValue(record.getDepartmentName())) segments.add("\u5339\u914d\u79d1\u5ba4\uff1a" + record.getDepartmentName());
        if (!isEmptyValue(record.getTriageSuggestion())) segments.add("\u7cfb\u7edf\u5efa\u8bae\uff1a" + record.getTriageSuggestion());
        if (!isEmptyValue(record.getTriageRuleSummary())) segments.add("\u98ce\u9669\u6458\u8981\uff1a" + record.getTriageRuleSummary());
        return abbreviate(String.join("\uff1b", segments), 1000);
    }

    private String buildTriageResultStructuredContent(ConsultationRecord record) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("triageLevelId", record.getTriageLevelId());
        payload.put("triageLevelCode", record.getTriageLevelCode());
        payload.put("triageLevelName", record.getTriageLevelName());
        payload.put("triageActionType", record.getTriageActionType());
        payload.put("triageSuggestion", record.getTriageSuggestion());
        payload.put("triageRuleSummary", record.getTriageRuleSummary());
        payload.put("departmentId", record.getDepartmentId());
        payload.put("departmentName", record.getDepartmentName());
        return JSON.toJSONString(payload);
    }

    private String buildRuleSummaryContent(ConsultationRecord record, List<TriageRuleHitSnapshot> ruleHits) {
        TriageRuleHitSnapshot primary = ruleHits.stream()
                .filter(item -> item.isPrimary() != null && item.isPrimary() == 1)
                .findFirst()
                .orElse(ruleHits.get(0));
        List<String> segments = new ArrayList<>();
        segments.add("\u547d\u4e2d\u89c4\u5219\uff1a" + ruleHits.size() + "\u6761");
        if (!isEmptyValue(primary.ruleName())) segments.add("\u4e3b\u89c4\u5219\uff1a" + primary.ruleName());
        if (!isEmptyValue(record.getTriageRuleSummary())) segments.add("\u7efc\u5408\u6458\u8981\uff1a" + record.getTriageRuleSummary());
        return abbreviate(String.join("\uff1b", segments), 1000);
    }

    private String buildRuleSummaryStructuredContent(List<TriageRuleHitSnapshot> ruleHits) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (TriageRuleHitSnapshot item : ruleHits) {
            payload.add(buildRuleHitPayload(item));
        }
        return JSON.toJSONString(payload);
    }

    private String buildRuleHitContent(TriageRuleHitSnapshot item) {
        List<String> segments = new ArrayList<>();
        segments.add("\u89e6\u53d1\u65b9\u5f0f\uff1a" + triggerTypeText(item.triggerType()));
        if (!isEmptyValue(item.matchedSummary())) segments.add("\u547d\u4e2d\u4f9d\u636e\uff1a" + item.matchedSummary());
        if (!isEmptyValue(item.triageLevelName())) segments.add("\u5bf9\u5e94\u7b49\u7ea7\uff1a" + item.triageLevelName());
        if (!isEmptyValue(item.actionType())) segments.add("\u5efa\u8bae\u52a8\u4f5c\uff1a" + triageActionText(item.actionType()));
        if (!isEmptyValue(item.suggestion())) segments.add("\u89c4\u5219\u5efa\u8bae\uff1a" + item.suggestion());
        return abbreviate(String.join("\uff1b", segments), 1000);
    }

    private String buildRuleHitStructuredContent(TriageRuleHitSnapshot item) {
        return JSON.toJSONString(buildRuleHitPayload(item));
    }

    private Map<String, Object> buildRuleHitPayload(TriageRuleHitSnapshot item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ruleId", item.ruleId());
        payload.put("ruleName", item.ruleName());
        payload.put("ruleCode", item.ruleCode());
        payload.put("triggerType", item.triggerType());
        payload.put("triageLevelId", item.triageLevelId());
        payload.put("triageLevelCode", item.triageLevelCode());
        payload.put("triageLevelName", item.triageLevelName());
        payload.put("actionType", item.actionType());
        payload.put("suggestion", item.suggestion());
        payload.put("matchedSummary", item.matchedSummary());
        payload.put("priority", item.priority());
        payload.put("isPrimary", item.isPrimary());
        return payload;
    }

    private String triageActionText(String actionType) {
        return switch (trimToNull(actionType) == null ? "" : actionType.toLowerCase()) {
            case "emergency" -> "\u7acb\u5373\u6025\u8bca";
            case "offline" -> "\u5c3d\u5feb\u7ebf\u4e0b\u5c31\u533b";
            case "followup" -> "\u590d\u8bca\u968f\u8bbf";
            case "online" -> "\u7ebf\u4e0a\u7ee7\u7eed";
            default -> "\u7ee7\u7eed\u5173\u6ce8";
        };
    }

    private String triggerTypeText(String triggerType) {
        return switch (trimToNull(triggerType) == null ? "" : triggerType.toLowerCase()) {
            case "symptom_match" -> "\u75c7\u72b6\u5339\u914d";
            case "keyword_match" -> "\u5173\u952e\u8bcd\u5339\u914d";
            case "body_part_match" -> "\u90e8\u4f4d\u5339\u914d";
            case "combination" -> "\u7ec4\u5408\u89c4\u5219";
            default -> "\u5176\u4ed6\u89e6\u53d1";
        };
    }

    private String saveTriageRuleHitLogs(Integer consultationId, List<TriageRuleHitSnapshot> ruleHits, Date now) {
        if (consultationId == null || ruleHits == null || ruleHits.isEmpty()) return null;
        for (TriageRuleHitSnapshot snapshot : ruleHits) {
            TriageRuleHitLog log = new TriageRuleHitLog(
                    null,
                    consultationId,
                    snapshot.ruleId(),
                    snapshot.ruleName(),
                    snapshot.ruleCode(),
                    snapshot.triggerType(),
                    snapshot.triageLevelId(),
                    snapshot.triageLevelCode(),
                    snapshot.triageLevelName(),
                    snapshot.actionType(),
                    snapshot.suggestion(),
                    snapshot.matchedSummary(),
                    snapshot.priority(),
                    snapshot.isPrimary(),
                    now
            );
            if (triageRuleHitLogMapper.insert(log) <= 0) {
                return "分诊规则命中日志保存失败，请稍后重试";
            }
        }
        return null;
    }

    private List<ConsultationRecommendDoctorVO> buildRecommendedDoctors(ConsultationRecord record) {
        if (record.getDepartmentId() == null) return List.of();
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
            if (remainingCapacity(schedule) > 0) nextAvailableScheduleMap.putIfAbsent(schedule.getDoctorId(), schedule);
        }

        Map<Integer, List<String>> tagMap = doctorServiceTagMapper.selectList(Wrappers.<DoctorServiceTag>query()
                        .in("doctor_id", doctorIds)
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .collect(Collectors.groupingBy(DoctorServiceTag::getDoctorId,
                        Collectors.mapping(DoctorServiceTag::getTagName, Collectors.toList())));

        return doctors.stream()
                .sorted(Comparator
                        .comparing((Doctor item) -> nextAvailableScheduleMap.containsKey(item.getId())).reversed()
                        .thenComparingLong(item -> {
                            DoctorSchedule schedule = nextAvailableScheduleMap.getOrDefault(item.getId(), nextScheduleMap.get(item.getId()));
                            return schedule == null ? Long.MAX_VALUE : schedule.getScheduleDate().getTime();
                        })
                        .thenComparingInt(item -> {
                            DoctorSchedule schedule = nextAvailableScheduleMap.getOrDefault(item.getId(), nextScheduleMap.get(item.getId()));
                            return schedule == null ? Integer.MAX_VALUE : scheduleTimeOrder(schedule.getTimePeriod());
                        })
                        .thenComparingInt(item -> defaultSort(item.getSort()))
                        .thenComparingInt(Doctor::getId))
                .limit(3)
                .map(item -> {
                    DoctorSchedule schedule = nextAvailableScheduleMap.getOrDefault(item.getId(), nextScheduleMap.get(item.getId()));
                    ConsultationRecommendDoctorVO vo = new ConsultationRecommendDoctorVO();
                    vo.setId(item.getId());
                    vo.setName(item.getName());
                    vo.setTitle(item.getTitle());
                    vo.setPhoto(item.getPhoto());
                    vo.setExpertise(item.getExpertise());
                    vo.setIntroduction(item.getIntroduction());
                    vo.setNextScheduleText(buildScheduleText(schedule));
                    vo.setRemainingCapacity(schedule == null ? null : remainingCapacity(schedule));
                    vo.setServiceTags(tagMap.getOrDefault(item.getId(), List.of()));
                    return vo;
                })
                .toList();
    }

    private String buildScheduleText(DoctorSchedule schedule) {
        if (schedule == null) return "暂未配置后续排班";
        return SCHEDULE_DATE_FORMAT.format(schedule.getScheduleDate())
                + " "
                + timePeriodLabel(schedule.getTimePeriod())
                + " · "
                + visitTypeLabel(schedule.getVisitType())
                + " · "
                + (remainingCapacity(schedule) > 0 ? "剩余 " + remainingCapacity(schedule) + " 号" : "当前号源已满");
    }

    private int remainingCapacity(DoctorSchedule schedule) {
        return Math.max(defaultInt(schedule.getMaxCapacity()) - defaultInt(schedule.getUsedCapacity()), 0);
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

    private String generateConsultationNo() {
        for (int i = 0; i < 6; i++) {
            String candidate = "C"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + ThreadLocalRandom.current().nextInt(1000, 9999);
            boolean exists = consultationRecordMapper.exists(Wrappers.<ConsultationRecord>query()
                    .eq("consultation_no", candidate));
            if (!exists) return candidate;
        }
        return "C" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String generateTriageSessionNo() {
        for (int i = 0; i < 6; i++) {
            String candidate = "TS"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + ThreadLocalRandom.current().nextInt(1000, 9999);
            boolean exists = triageSessionMapper.exists(Wrappers.<TriageSession>query()
                    .eq("session_no", candidate));
            if (!exists) return candidate;
        }
        return "TS" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
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

    private boolean isEmptyValue(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int defaultSort(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private record NormalizeResult(String value, String message) {
    }

    private record RuleMatch(RedFlagRule rule, TriageLevelDict level, String matchedSummary) {
    }

    private record TriageRuleHitSnapshot(Integer ruleId,
                                         String ruleName,
                                         String ruleCode,
                                         String triggerType,
                                         Integer triageLevelId,
                                         String triageLevelCode,
                                         String triageLevelName,
                                         String actionType,
                                         String suggestion,
                                         String matchedSummary,
                                         Integer priority,
                                         Integer isPrimary) {
    }

    private record TriageMessageDraft(String roleType,
                                      String messageType,
                                      String title,
                                      String content,
                                      String structuredContent) {
    }

    private record TriageDecision(Integer levelId,
                                  String levelCode,
                                  String levelName,
                                  String levelColor,
                                  String actionType,
                                  String suggestion,
                                  String ruleSummary,
                                  List<TriageRuleHitSnapshot> ruleHits) {
    }
}
