package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.ai.advisor.MedicalLongTermMemoryAdvisor;
import cn.gugufish.entity.dto.PatientMedicalHistory;
import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.vo.request.ConsultationIntakeRouteRequestVO;
import cn.gugufish.entity.vo.response.ConsultationEntryCategoryVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeRouteVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.mapper.PatientMedicalHistoryMapper;
import cn.gugufish.mapper.PatientProfileMapper;
import cn.gugufish.service.ConsultationIntakeRoutingService;
import cn.gugufish.service.ConsultationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class ConsultationIntakeRoutingServiceImpl implements ConsultationIntakeRoutingService {

    private final ConsultationService consultationService;
    private final PatientProfileMapper patientProfileMapper;
    private final PatientMedicalHistoryMapper patientMedicalHistoryMapper;
    private final ObjectProvider<ChatClient> advisorChatClientProvider;
    private final AiTriageProperties aiTriageProperties;
    private final Environment environment;

    public ConsultationIntakeRoutingServiceImpl(ConsultationService consultationService,
                                                PatientProfileMapper patientProfileMapper,
                                                PatientMedicalHistoryMapper patientMedicalHistoryMapper,
                                                ObjectProvider<ChatClient> advisorChatClientProvider,
                                                AiTriageProperties aiTriageProperties,
                                                Environment environment) {
        this.consultationService = consultationService;
        this.patientProfileMapper = patientProfileMapper;
        this.patientMedicalHistoryMapper = patientMedicalHistoryMapper;
        this.advisorChatClientProvider = advisorChatClientProvider;
        this.aiTriageProperties = aiTriageProperties;
        this.environment = environment;
    }

    @Override
    public ConsultationIntakeRouteVO route(int accountId, ConsultationIntakeRouteRequestVO vo) {
        String chiefComplaint = trimToNull(vo == null ? null : vo.getChiefComplaint());
        if (chiefComplaint == null) return null;
        String matchMode = normalizeMatchMode(vo == null ? null : vo.getMatchMode());

        List<ConsultationEntryCategoryVO> categories = consultationService.listEntryCategories();
        if (categories.isEmpty()) return null;

        PatientProfile patient = loadPatient(accountId, vo == null ? null : vo.getPatientId());
        PatientMedicalHistory history = patient == null ? null : patientMedicalHistoryMapper.selectOne(
                Wrappers.<PatientMedicalHistory>query().eq("patient_id", patient.getId())
        );

        CategoryMatchResult match = resolveCategory(matchMode, chiefComplaint, patient, history, categories);
        ConsultationEntryCategoryVO category = match.category() == null ? fallbackCategory(categories) : match.category();
        if (category == null) return null;

        ConsultationIntakeTemplateVO template = consultationService.defaultTemplateDetail(category.getId());
        if (template == null) return null;

        ConsultationIntakeRouteVO result = new ConsultationIntakeRouteVO();
        result.setChiefComplaint(chiefComplaint);
        result.setMatchMode(matchMode);
        result.setMatchModeLabel(matchModeLabel(matchMode));
        result.setSelectionMode(match.selectionMode());
        result.setSelectionModeLabel(selectionModeLabel(match.selectionMode()));
        result.setRouteReason(match.reason());
        result.setConfidenceScore(normalizeConfidence(match.confidenceScore(), BigDecimal.valueOf(0.60)));
        result.setCategory(category);
        result.setTemplate(template);
        return result;
    }

    private CategoryMatchResult resolveCategory(String matchMode,
                                                String chiefComplaint,
                                                PatientProfile patient,
                                                PatientMedicalHistory history,
                                                List<ConsultationEntryCategoryVO> categories) {
        if ("quick".equals(matchMode)) {
            return resolveByRules(chiefComplaint, patient, categories);
        }

        CategoryMatchResult aiMatch = resolveByAi(chiefComplaint, patient, history, categories);
        if (aiMatch != null && aiMatch.category() != null) return aiMatch;

        CategoryMatchResult ruleMatch = resolveByRules(chiefComplaint, patient, categories);
        if (ruleMatch == null) return null;

        return new CategoryMatchResult(
                ruleMatch.category(),
                "ai_fallback_rule",
                abbreviate("智能匹配暂未稳定返回，已自动切换为规则匹配结果：" + defaultText(ruleMatch.reason(), "系统已匹配到当前最接近的问诊方向。"), 120),
                normalizeConfidence(ruleMatch.confidenceScore(), BigDecimal.valueOf(0.68))
        );
    }

    private CategoryMatchResult resolveByAi(String chiefComplaint,
                                            PatientProfile patient,
                                            PatientMedicalHistory history,
                                            List<ConsultationEntryCategoryVO> categories) {
        if (!isAiAvailable()) return null;

        try {
            ChatClient chatClient = advisorChatClientProvider.getIfAvailable();
            if (chatClient == null) return null;

            CategoryRoutePayload payload = chatClient.prompt()
                    .system(buildRoutingSystemPrompt())
                    .user(buildRoutingUserPrompt(chiefComplaint, patient, history, categories))
                    .advisors(spec -> {
                        spec.param(ChatMemory.CONVERSATION_ID, "intake-route-" + UUID.randomUUID());
                        if (patient != null && StringUtils.hasText(patient.getName())) {
                            spec.param(MedicalLongTermMemoryAdvisor.PATIENT_ID_KEY,
                                    "patient-" + patient.getName().hashCode());
                        }
                    })
                    .call()
                    .entity(CategoryRoutePayload.class);

            if (payload == null) return null;
            ConsultationEntryCategoryVO category = matchCategory(payload, chiefComplaint, patient, categories);
            if (category == null) return null;

            return new CategoryMatchResult(
                    category,
                    "ai_match",
                    abbreviate(defaultText(payload.getReason(), "系统已根据症状描述匹配到更合适的问诊方向。"), 120),
                    normalizeConfidence(payload.getConfidenceScore(), BigDecimal.valueOf(0.82))
            );
        } catch (Exception exception) {
            log.warn("Intake route AI match failed: {}", exception.getMessage());
            return null;
        }
    }

    private CategoryMatchResult resolveByRules(String chiefComplaint,
                                               PatientProfile patient,
                                               List<ConsultationEntryCategoryVO> categories) {
        String normalizedText = normalizeText(chiefComplaint);
        Integer age = calculateAge(patient == null ? null : patient.getBirthDate());
        boolean childPatient = age != null && age <= 14;
        boolean femalePatient = "female".equalsIgnoreCase(trimToEmpty(patient == null ? null : patient.getGender()));

        if (containsAny(normalizedText, "报告", "化验", "检查", "检验", "片子", "CT", "核磁", "B超", "彩超")) {
            ConsultationEntryCategoryVO category = findCategoryByCode(categories, "REPORT_INTERPRET");
            if (category != null) {
                return new CategoryMatchResult(category, "rule_match",
                        "已根据检查结果或报告解读相关描述匹配到报告解读场景。", BigDecimal.valueOf(0.86));
            }
        }

        if (containsAny(normalizedText, "高血压", "糖尿病", "慢病", "复诊", "续方", "血压", "血糖", "长期用药")) {
            ConsultationEntryCategoryVO category = findCategoryByCode(categories, "CHRONIC_FOLLOWUP");
            if (category != null) {
                return new CategoryMatchResult(category, "rule_match",
                        "已根据慢病管理或复诊相关描述匹配到慢病复诊场景。", BigDecimal.valueOf(0.84));
            }
        }

        if (containsAny(normalizedText, "皮疹", "瘙痒", "湿疹", "痘", "痤疮", "皮肤", "过敏", "荨麻疹", "脱皮")) {
            ConsultationEntryCategoryVO category = findCategoryByCode(categories, "SKIN_ISSUE");
            if (category != null) {
                return new CategoryMatchResult(category, "rule_match",
                        "已根据皮肤症状相关描述匹配到皮肤问题咨询场景。", BigDecimal.valueOf(0.83));
            }
        }

        if (containsAny(normalizedText, "月经", "妇科", "白带", "备孕", "阴道", "痛经", "经期", "分泌物")
                || (femalePatient && containsAny(normalizedText, "下腹痛", "盆腔", "排卵"))) {
            ConsultationEntryCategoryVO category = findCategoryByCode(categories, "WOMEN_HEALTH");
            if (category != null) {
                return new CategoryMatchResult(category, "rule_match",
                        "已根据女性健康相关描述匹配到女性健康咨询场景。", BigDecimal.valueOf(0.83));
            }
        }

        if ((childPatient || containsAny(normalizedText, "孩子", "宝宝", "儿童", "婴儿", "小孩"))
                && containsAny(normalizedText, "发热", "发烧", "咳嗽", "流鼻涕", "精神差", "喉咙", "嗓子")) {
            ConsultationEntryCategoryVO category = findCategoryByCode(categories, "PEDI_FEVER");
            if (category != null) {
                return new CategoryMatchResult(category, "rule_match",
                        "已根据儿童发热相关描述匹配到儿科问诊场景。", BigDecimal.valueOf(0.82));
            }
        }

        ConsultationEntryCategoryVO fallback = fallbackCategory(categories);
        return new CategoryMatchResult(
                fallback,
                "default_fallback",
                "当前描述尚不足以锁定具体专科，已先进入综合问诊入口继续分诊。",
                BigDecimal.valueOf(0.65)
        );
    }

    private ConsultationEntryCategoryVO matchCategory(CategoryRoutePayload payload,
                                                      String chiefComplaint,
                                                      PatientProfile patient,
                                                      List<ConsultationEntryCategoryVO> categories) {
        ConsultationEntryCategoryVO byCode = findCategoryByCode(categories, payload.getCategoryCode());
        if (byCode != null) return byCode;

        ConsultationEntryCategoryVO byName = findCategoryByName(categories, payload.getCategoryName());
        if (byName != null) return byName;

        ConsultationEntryCategoryVO byDepartment = findCategoryByDepartment(categories, payload.getDepartmentName(), chiefComplaint, patient);
        if (byDepartment != null) return byDepartment;

        return null;
    }

    private ConsultationEntryCategoryVO findCategoryByCode(List<ConsultationEntryCategoryVO> categories, String code) {
        String target = trimToUpper(code);
        if (target == null) return null;
        return categories.stream()
                .filter(item -> target.equals(trimToUpper(item.getCode())))
                .findFirst()
                .orElse(null);
    }

    private ConsultationEntryCategoryVO findCategoryByName(List<ConsultationEntryCategoryVO> categories, String name) {
        String target = normalizeText(name);
        if (target == null) return null;
        return categories.stream()
                .filter(item -> {
                    String current = normalizeText(item.getName());
                    return current != null && (current.equals(target) || current.contains(target) || target.contains(current));
                })
                .findFirst()
                .orElse(null);
    }

    private ConsultationEntryCategoryVO findCategoryByDepartment(List<ConsultationEntryCategoryVO> categories,
                                                                 String departmentName,
                                                                 String chiefComplaint,
                                                                 PatientProfile patient) {
        String target = normalizeText(departmentName);
        if (target == null) return null;

        List<ConsultationEntryCategoryVO> matched = categories.stream()
                .filter(item -> {
                    String current = normalizeText(item.getDepartmentName());
                    return current != null && (current.equals(target) || current.contains(target) || target.contains(current));
                })
                .toList();
        if (matched.isEmpty()) return null;
        if (matched.size() == 1) return matched.get(0);

        CategoryMatchResult narrowed = resolveByRules(chiefComplaint, patient, matched);
        if (narrowed != null && narrowed.category() != null && matched.contains(narrowed.category())) {
            return narrowed.category();
        }
        return matched.stream()
                .filter(item -> "TEXT_CONSULT".equalsIgnoreCase(trimToEmpty(item.getCode())))
                .findFirst()
                .orElse(null);
    }

    private ConsultationEntryCategoryVO fallbackCategory(List<ConsultationEntryCategoryVO> categories) {
        ConsultationEntryCategoryVO general = findCategoryByCode(categories, "TEXT_CONSULT");
        if (general != null) return general;
        return categories.stream().findFirst().orElse(null);
    }

    private String normalizeMatchMode(String value) {
        String mode = trimToNull(value);
        if (mode == null) return "ai";
        mode = mode.toLowerCase(Locale.ROOT);
        return "quick".equals(mode) ? "quick" : "ai";
    }

    private String matchModeLabel(String mode) {
        return "quick".equals(mode) ? "快速匹配" : "智能匹配";
    }

    private String selectionModeLabel(String mode) {
        if ("ai_match".equals(mode)) return "AI匹配";
        if ("ai_fallback_rule".equals(mode)) return "AI不可用，已切换规则匹配";
        if ("rule_match".equals(mode)) return "规则匹配";
        if ("default_fallback".equals(mode)) return "综合入口兜底";
        return "系统匹配";
    }

    private PatientProfile loadPatient(int accountId, Integer patientId) {
        if (patientId == null) return null;
        return patientProfileMapper.selectOne(Wrappers.<PatientProfile>query()
                .eq("id", patientId)
                .eq("account_id", accountId)
                .eq("status", 1));
    }

    private boolean isAiAvailable() {
        return aiTriageProperties.isEnabled()
                && hasApiKey()
                && advisorChatClientProvider.getIfAvailable() != null;
    }

    private boolean hasApiKey() {
        return StringUtils.hasText(environment.getProperty("spring.ai.deepseek.api-key"))
                || StringUtils.hasText(environment.getProperty("spring.ai.deepseek.chat.api-key"));
    }

    private String buildRoutingSystemPrompt() {
        return """
                你是智慧医疗问诊系统的入口分诊助手。
                你的任务不是直接诊断疾病，而是根据用户的简短描述，在系统已经存在的问诊场景中选择最合适的一个。
                你必须严格遵守以下规则：
                1. 只能从系统提供的真实问诊分类中选择，禁止编造新的分类编码、分类名称或科室名称。
                2. 如果信息足以判断为报告解读、慢病复诊、儿科发热、女性健康或皮肤问题，应优先返回对应场景。
                3. 如果信息仍然不足以明确归属，必须返回综合入口分类 TEXT_CONSULT。
                4. 只输出结构化结果，不要输出额外解释文本。
                """;
    }

    private String buildRoutingUserPrompt(String chiefComplaint,
                                          PatientProfile patient,
                                          PatientMedicalHistory history,
                                          List<ConsultationEntryCategoryVO> categories) {
        Integer age = calculateAge(patient == null ? null : patient.getBirthDate());
        return """
                请根据以下信息，为用户匹配最合适的问诊分类。

                用户当前描述：
                %s

                就诊人信息：
                - 姓名：%s
                - 性别：%s
                - 年龄：%s

                健康档案摘要：
                %s

                系统可选问诊分类：
                %s

                输出要求：
                - categoryCode：必须填写系统真实存在的分类编码。
                - categoryName：必须填写与分类编码对应的真实分类名称。
                - departmentName：必须填写与分类对应的真实科室名称。
                - reason：简要说明为什么这样匹配，控制在60字以内。
                - confidenceScore：填写0到1之间的小数。
                """.formatted(
                chiefComplaint,
                defaultText(patient == null ? null : patient.getName(), "未选择"),
                defaultText(genderLabel(patient == null ? null : patient.getGender()), "未提供"),
                age == null ? "未提供" : String.valueOf(age),
                buildHistorySummary(history),
                buildCategoryOptions(categories)
        );
    }

    private String buildCategoryOptions(List<ConsultationEntryCategoryVO> categories) {
        return categories.stream()
                .map(item -> "- categoryCode=%s；categoryName=%s；departmentName=%s；description=%s；template=%s".formatted(
                        defaultText(item.getCode(), "-"),
                        defaultText(item.getName(), "-"),
                        defaultText(item.getDepartmentName(), "-"),
                        abbreviate(defaultText(item.getDescription(), item.getDefaultTemplateDescription()), 72),
                        defaultText(item.getDefaultTemplateName(), "-")
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- categoryCode=TEXT_CONSULT；categoryName=图文问诊；departmentName=全科门诊");
    }

    private String buildHistorySummary(PatientMedicalHistory history) {
        if (history == null) return "暂无健康档案信息";
        StringBuilder builder = new StringBuilder();
        appendSummary(builder, "过敏史", history.getAllergyHistory());
        appendSummary(builder, "慢病史", history.getChronicHistory());
        appendSummary(builder, "既往史", history.getPastHistory());
        appendSummary(builder, "长期用药", history.getMedicationHistory());
        return builder.length() == 0 ? "暂无健康档案信息" : builder.toString();
    }

    private void appendSummary(StringBuilder builder, String label, String value) {
        String text = trimToNull(value);
        if (text == null) return;
        if (builder.length() > 0) builder.append("；");
        builder.append(label).append("：").append(abbreviate(text, 48));
    }

    private Integer calculateAge(java.util.Date birthDate) {
        if (birthDate == null) return null;
        LocalDate date = Instant.ofEpochMilli(birthDate.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return (int) ChronoUnit.YEARS.between(date, LocalDate.now());
    }

    private String genderLabel(String gender) {
        return switch (trimToEmpty(gender).toLowerCase(Locale.ROOT)) {
            case "male" -> "男";
            case "female" -> "女";
            default -> "未知";
        };
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) return false;
        for (String keyword : keywords) {
            String current = normalizeText(keyword);
            if (current != null && text.contains(current)) return true;
        }
        return false;
    }

    private BigDecimal normalizeConfidence(BigDecimal value, BigDecimal fallback) {
        BigDecimal confidence = value == null ? fallback : value;
        if (confidence.compareTo(BigDecimal.ZERO) < 0) confidence = BigDecimal.ZERO;
        if (confidence.compareTo(BigDecimal.ONE) > 0) confidence = BigDecimal.ONE;
        return confidence.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeText(String value) {
        String text = trimToNull(value);
        if (text == null) return null;
        return text
                .replace("门诊", "")
                .replace("咨询", "")
                .replace("问诊", "")
                .replace("模板", "")
                .replace("科室", "")
                .replace("专科", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
    }

    private String trimToUpper(String value) {
        String text = trimToNull(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultText(String value, String fallback) {
        String text = trimToNull(value);
        return text == null ? fallback : text;
    }

    private String abbreviate(String value, int maxLength) {
        String text = trimToNull(value);
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private record CategoryMatchResult(ConsultationEntryCategoryVO category,
                                       String selectionMode,
                                       String reason,
                                       BigDecimal confidenceScore) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryRoutePayload {
        private String categoryCode;
        private String categoryName;
        private String departmentName;
        private String reason;
        private BigDecimal confidenceScore;

        public String getCategoryCode() {
            return categoryCode;
        }

        public void setCategoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public BigDecimal getConfidenceScore() {
            return confidenceScore;
        }

        public void setConfidenceScore(BigDecimal confidenceScore) {
            this.confidenceScore = confidenceScore;
        }
    }
}
