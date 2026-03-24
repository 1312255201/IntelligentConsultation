package cn.gugufish.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import cn.gugufish.entity.dto.ConsultationCategory;
import cn.gugufish.entity.dto.ConsultationIntakeField;
import cn.gugufish.entity.dto.ConsultationIntakeTemplate;
import cn.gugufish.entity.vo.request.ConsultationIntakeFieldSaveVO;
import cn.gugufish.entity.vo.request.ConsultationIntakeTemplateCreateVO;
import cn.gugufish.entity.vo.request.ConsultationIntakeTemplateUpdateVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeFieldVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.mapper.ConsultationCategoryMapper;
import cn.gugufish.mapper.ConsultationIntakeFieldMapper;
import cn.gugufish.mapper.ConsultationIntakeTemplateMapper;
import cn.gugufish.service.ConsultationIntakeTemplateService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConsultationIntakeTemplateServiceImpl implements ConsultationIntakeTemplateService {

    private static final Set<String> ALLOWED_FIELD_TYPES = Set.of(
            "input",
            "textarea",
            "single_select",
            "multi_select",
            "date",
            "number",
            "upload",
            "switch"
    );
    private static final Set<String> OPTION_FIELD_TYPES = Set.of("single_select", "multi_select");

    @Resource
    ConsultationIntakeTemplateMapper intakeTemplateMapper;

    @Resource
    ConsultationIntakeFieldMapper intakeFieldMapper;

    @Resource
    ConsultationCategoryMapper categoryMapper;

    @Override
    public List<ConsultationIntakeTemplateVO> listTemplates() {
        List<ConsultationIntakeTemplate> templates = intakeTemplateMapper.selectList(Wrappers.<ConsultationIntakeTemplate>query()
                .orderByAsc("category_id")
                .orderByDesc("is_default")
                .orderByDesc("version")
                .orderByDesc("id"));
        if (templates.isEmpty()) return List.of();

        List<Integer> templateIds = templates.stream().map(ConsultationIntakeTemplate::getId).toList();
        var fieldCountMap = intakeFieldMapper.selectList(Wrappers.<ConsultationIntakeField>query()
                        .in("template_id", templateIds))
                .stream()
                .collect(Collectors.groupingBy(ConsultationIntakeField::getTemplateId, Collectors.counting()));

        return templates.stream()
                .map(item -> item.asViewObject(ConsultationIntakeTemplateVO.class, vo ->
                        vo.setFieldCount(fieldCountMap.getOrDefault(item.getId(), 0L).intValue())))
                .toList();
    }

    @Override
    public ConsultationIntakeTemplateVO templateDetail(int id) {
        ConsultationIntakeTemplate template = intakeTemplateMapper.selectById(id);
        if (template == null) return null;

        List<ConsultationIntakeFieldVO> fields = intakeFieldMapper.selectList(Wrappers.<ConsultationIntakeField>query()
                        .eq("template_id", id)
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
    @Transactional
    public String createTemplate(ConsultationIntakeTemplateCreateVO vo) {
        String message = validateTemplate(vo, null);
        if (message != null) return message;

        Date now = new Date();
        ConsultationIntakeTemplate template = new ConsultationIntakeTemplate(
                null,
                vo.getCategoryId(),
                vo.getName().trim(),
                blankToNull(vo.getDescription()),
                vo.getVersion(),
                vo.getIsDefault(),
                vo.getStatus(),
                now,
                now
        );
        if (intakeTemplateMapper.insert(template) <= 0) {
            return "问诊前置模板新增失败，请联系管理员";
        }

        String fieldMessage = saveFields(template.getId(), vo.getFields(), now);
        if (fieldMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return fieldMessage;
        }

        if (template.getIsDefault() != null && template.getIsDefault() == 1) {
            clearOtherDefaultTemplates(template.getCategoryId(), template.getId());
        }
        ensureCategoryHasDefault(template.getCategoryId());
        return null;
    }

    @Override
    @Transactional
    public String updateTemplate(ConsultationIntakeTemplateUpdateVO vo) {
        ConsultationIntakeTemplate current = intakeTemplateMapper.selectById(vo.getId());
        if (current == null) return "问诊前置模板不存在";

        String message = validateTemplate(vo, vo.getId());
        if (message != null) return message;

        ConsultationIntakeTemplate template = new ConsultationIntakeTemplate(
                vo.getId(),
                vo.getCategoryId(),
                vo.getName().trim(),
                blankToNull(vo.getDescription()),
                vo.getVersion(),
                vo.getIsDefault(),
                vo.getStatus(),
                current.getCreateTime(),
                new Date()
        );
        if (intakeTemplateMapper.updateById(template) <= 0) {
            return "问诊前置模板更新失败，请联系管理员";
        }

        intakeFieldMapper.delete(Wrappers.<ConsultationIntakeField>query().eq("template_id", vo.getId()));
        String fieldMessage = saveFields(vo.getId(), vo.getFields(), new Date());
        if (fieldMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return fieldMessage;
        }

        if (template.getIsDefault() != null && template.getIsDefault() == 1) {
            clearOtherDefaultTemplates(template.getCategoryId(), template.getId());
        }
        ensureCategoryHasDefault(current.getCategoryId());
        if (!current.getCategoryId().equals(template.getCategoryId())) {
            ensureCategoryHasDefault(template.getCategoryId());
        }
        return null;
    }

    @Override
    @Transactional
    public String deleteTemplate(int id) {
        ConsultationIntakeTemplate current = intakeTemplateMapper.selectById(id);
        if (current == null) return "问诊前置模板不存在";
        if (intakeTemplateMapper.deleteById(id) <= 0) {
            return "问诊前置模板删除失败，请联系管理员";
        }
        ensureCategoryHasDefault(current.getCategoryId());
        return null;
    }

    private String validateTemplate(ConsultationIntakeTemplateCreateVO vo, Integer ignoreId) {
        ConsultationCategory category = categoryMapper.selectById(vo.getCategoryId());
        if (category == null) return "关联的问诊分类不存在";
        if (existsSameTemplate(vo.getCategoryId(), vo.getName(), vo.getVersion(), ignoreId)) {
            return "同一问诊分类下已存在相同名称和版本号的前置模板";
        }
        if (vo.getFields() == null || vo.getFields().isEmpty()) {
            return "请至少配置一个采集字段";
        }

        Set<String> fieldCodes = new HashSet<>();
        for (int index = 0; index < vo.getFields().size(); index++) {
            ConsultationIntakeFieldSaveVO field = vo.getFields().get(index);
            String normalizedCode = field.getFieldCode().trim().toLowerCase();
            if (!fieldCodes.add(normalizedCode)) {
                return "字段编码存在重复，请检查第 " + (index + 1) + " 个字段";
            }
            if (!ALLOWED_FIELD_TYPES.contains(field.getFieldType())) {
                return "存在不支持的字段类型，请检查第 " + (index + 1) + " 个字段";
            }
            if (OPTION_FIELD_TYPES.contains(field.getFieldType())) {
                String optionsMessage = validateFieldOptions(field);
                if (optionsMessage != null) return optionsMessage;
            }
        }
        return null;
    }

    private String validateFieldOptions(ConsultationIntakeFieldSaveVO field) {
        if (field.getOptionsJson() == null || field.getOptionsJson().isBlank()) {
            return "字段“" + field.getFieldLabel().trim() + "”需要至少配置一个选项";
        }
        try {
            JSONArray array = JSON.parseArray(field.getOptionsJson());
            List<String> options = array.stream()
                    .map(item -> item == null ? null : String.valueOf(item).trim())
                    .filter(item -> item != null && !item.isEmpty())
                    .distinct()
                    .toList();
            if (options.isEmpty()) {
                return "字段“" + field.getFieldLabel().trim() + "”需要至少配置一个选项";
            }
        } catch (Exception exception) {
            return "字段“" + field.getFieldLabel().trim() + "”的选项格式不正确";
        }
        return null;
    }

    private String saveFields(int templateId, List<ConsultationIntakeFieldSaveVO> fields, Date now) {
        for (ConsultationIntakeFieldSaveVO field : fields) {
            ConsultationIntakeField entity = new ConsultationIntakeField(
                    null,
                    templateId,
                    field.getFieldCode().trim(),
                    field.getFieldLabel().trim(),
                    field.getFieldType(),
                    field.getIsRequired(),
                    normalizeOptionsJson(field),
                    blankToNull(field.getDefaultValue()),
                    blankToNull(field.getPlaceholder()),
                    blankToNull(field.getHelpText()),
                    blankToNull(field.getConditionRule()),
                    blankToNull(field.getValidationRule()),
                    field.getSort(),
                    field.getStatus(),
                    now,
                    now
            );
            if (intakeFieldMapper.insert(entity) <= 0) {
                return "问诊前置字段保存失败，请联系管理员";
            }
        }
        return null;
    }

    private String normalizeOptionsJson(ConsultationIntakeFieldSaveVO field) {
        if (!OPTION_FIELD_TYPES.contains(field.getFieldType())) {
            return null;
        }
        JSONArray array = JSON.parseArray(field.getOptionsJson());
        List<String> options = array.stream()
                .map(item -> item == null ? null : String.valueOf(item).trim())
                .filter(item -> item != null && !item.isEmpty())
                .distinct()
                .toList();
        return JSON.toJSONString(options);
    }

    private boolean existsSameTemplate(int categoryId, String name, int version, Integer ignoreId) {
        return intakeTemplateMapper.exists(Wrappers.<ConsultationIntakeTemplate>query()
                .eq("category_id", categoryId)
                .eq("name", name.trim())
                .eq("version", version)
                .ne(ignoreId != null, "id", ignoreId));
    }

    private void clearOtherDefaultTemplates(int categoryId, int currentId) {
        intakeTemplateMapper.update(null, Wrappers.<ConsultationIntakeTemplate>update()
                .eq("category_id", categoryId)
                .eq("is_default", 1)
                .ne("id", currentId)
                .set("is_default", 0)
                .set("update_time", new Date()));
    }

    private void ensureCategoryHasDefault(int categoryId) {
        List<ConsultationIntakeTemplate> templates = intakeTemplateMapper.selectList(Wrappers.<ConsultationIntakeTemplate>query()
                .eq("category_id", categoryId)
                .orderByDesc("status")
                .orderByDesc("version")
                .orderByDesc("id"));
        if (templates.isEmpty()) return;

        boolean hasDefault = templates.stream().anyMatch(item -> item.getIsDefault() != null && item.getIsDefault() == 1);
        if (hasDefault) return;

        ConsultationIntakeTemplate fallback = templates.getFirst();
        intakeTemplateMapper.update(null, Wrappers.<ConsultationIntakeTemplate>update()
                .eq("id", fallback.getId())
                .set("is_default", 1)
                .set("update_time", new Date()));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
