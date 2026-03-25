package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.BodyPartDict;
import cn.gugufish.entity.dto.RedFlagRule;
import cn.gugufish.entity.dto.RedFlagRuleSymptom;
import cn.gugufish.entity.dto.SymptomDict;
import cn.gugufish.entity.dto.TriageLevelDict;
import cn.gugufish.entity.vo.request.RedFlagRuleCreateVO;
import cn.gugufish.entity.vo.request.RedFlagRuleUpdateVO;
import cn.gugufish.entity.vo.response.RedFlagRuleVO;
import cn.gugufish.mapper.BodyPartDictMapper;
import cn.gugufish.mapper.RedFlagRuleMapper;
import cn.gugufish.mapper.RedFlagRuleSymptomMapper;
import cn.gugufish.mapper.SymptomDictMapper;
import cn.gugufish.mapper.TriageLevelDictMapper;
import cn.gugufish.service.RedFlagRuleService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RedFlagRuleServiceImpl extends ServiceImpl<RedFlagRuleMapper, RedFlagRule> implements RedFlagRuleService {

    @Resource
    BodyPartDictMapper bodyPartDictMapper;

    @Resource
    SymptomDictMapper symptomDictMapper;

    @Resource
    TriageLevelDictMapper triageLevelDictMapper;

    @Resource
    RedFlagRuleSymptomMapper redFlagRuleSymptomMapper;

    @Override
    public List<RedFlagRuleVO> listRules() {
        List<RedFlagRule> rules = this.list(Wrappers.<RedFlagRule>query()
                .orderByDesc("priority")
                .orderByAsc("id"));
        if (rules.isEmpty()) return List.of();

        List<Integer> ruleIds = rules.stream().map(RedFlagRule::getId).toList();
        Map<Integer, List<Integer>> symptomIdsMap = redFlagRuleSymptomMapper.selectList(Wrappers.<RedFlagRuleSymptom>query()
                        .in("rule_id", ruleIds))
                .stream()
                .collect(Collectors.groupingBy(RedFlagRuleSymptom::getRuleId,
                        Collectors.mapping(RedFlagRuleSymptom::getSymptomId, Collectors.toList())));

        return rules.stream()
                .map(item -> item.asViewObject(RedFlagRuleVO.class, vo ->
                        vo.setSymptomIds(symptomIdsMap.getOrDefault(item.getId(), List.of()))))
                .toList();
    }

    @Override
    @Transactional
    public String createRule(RedFlagRuleCreateVO vo) {
        List<Integer> symptomIds = normalizeSymptomIds(vo.getSymptomIds());
        String message = validateRule(vo, symptomIds, null);
        if (message != null) return message;

        Date now = new Date();
        RedFlagRule rule = new RedFlagRule(
                null,
                vo.getRuleName().trim(),
                vo.getRuleCode().trim(),
                vo.getTriggerType(),
                vo.getBodyPartId(),
                blankToNull(vo.getKeywordPattern()),
                blankToNull(vo.getConditionDescription()),
                vo.getTriageLevelId(),
                vo.getSuggestion().trim(),
                vo.getActionType(),
                vo.getPriority(),
                vo.getStatus(),
                now,
                now
        );
        if (!this.save(rule)) {
            return "红旗规则新增失败，请联系管理员";
        }

        String symptomMessage = saveRuleSymptoms(rule.getId(), symptomIds);
        if (symptomMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return symptomMessage;
        }
        return null;
    }

    @Override
    @Transactional
    public String updateRule(RedFlagRuleUpdateVO vo) {
        RedFlagRule current = this.getById(vo.getId());
        if (current == null) return "红旗规则不存在";

        List<Integer> symptomIds = normalizeSymptomIds(vo.getSymptomIds());
        String message = validateRule(vo, symptomIds, vo.getId());
        if (message != null) return message;

        boolean updated = this.update(Wrappers.<RedFlagRule>update()
                .eq("id", vo.getId())
                .set("rule_name", vo.getRuleName().trim())
                .set("rule_code", vo.getRuleCode().trim())
                .set("trigger_type", vo.getTriggerType())
                .set("body_part_id", vo.getBodyPartId())
                .set("keyword_pattern", blankToNull(vo.getKeywordPattern()))
                .set("condition_description", blankToNull(vo.getConditionDescription()))
                .set("triage_level_id", vo.getTriageLevelId())
                .set("suggestion", vo.getSuggestion().trim())
                .set("action_type", vo.getActionType())
                .set("priority", vo.getPriority())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        if (!updated) return "红旗规则更新失败，请联系管理员";

        redFlagRuleSymptomMapper.delete(Wrappers.<RedFlagRuleSymptom>query().eq("rule_id", vo.getId()));
        String symptomMessage = saveRuleSymptoms(vo.getId(), symptomIds);
        if (symptomMessage != null) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return symptomMessage;
        }
        return null;
    }

    @Override
    public String deleteRule(int id) {
        RedFlagRule current = this.getById(id);
        if (current == null) return "红旗规则不存在";
        return this.removeById(id) ? null : "红旗规则删除失败，请联系管理员";
    }

    private String validateRule(RedFlagRuleCreateVO vo, List<Integer> symptomIds, Integer ignoreId) {
        if (existsByCode(vo.getRuleCode(), ignoreId)) return "红旗规则编码已存在";
        if (!existsTriageLevel(vo.getTriageLevelId())) return "关联紧急等级不存在";

        if (vo.getBodyPartId() != null) {
            BodyPartDict bodyPart = bodyPartDictMapper.selectById(vo.getBodyPartId());
            if (bodyPart == null) return "关联部位不存在";
        }

        List<SymptomDict> symptoms = findSymptoms(symptomIds);
        if (symptoms.size() != symptomIds.size()) return "存在无效的症状字典，请检查后重试";

        if (vo.getBodyPartId() != null && !symptoms.isEmpty()) {
            boolean matched = symptoms.stream().allMatch(item -> vo.getBodyPartId().equals(item.getBodyPartId()));
            if (!matched) return "已选择的症状必须属于当前选中的部位";
        }

        int conditionCount = 0;
        if (vo.getBodyPartId() != null) conditionCount++;
        if (!symptomIds.isEmpty()) conditionCount++;
        if (vo.getKeywordPattern() != null && !vo.getKeywordPattern().trim().isEmpty()) conditionCount++;

        return switch (vo.getTriggerType()) {
            case "symptom_match" -> symptomIds.isEmpty() ? "症状匹配规则至少需要选择一个症状" : null;
            case "keyword_match" -> blankToNull(vo.getKeywordPattern()) == null ? "关键词匹配规则请填写关键词模式" : null;
            case "body_part_match" -> vo.getBodyPartId() == null ? "部位匹配规则请选择身体部位" : null;
            case "combination" -> conditionCount < 2 ? "组合规则至少需要配置两类触发条件" : null;
            default -> "不支持的规则触发类型";
        };
    }

    private List<Integer> normalizeSymptomIds(List<Integer> symptomIds) {
        if (symptomIds == null || symptomIds.isEmpty()) return List.of();
        return symptomIds.stream().distinct().toList();
    }

    private List<SymptomDict> findSymptoms(List<Integer> symptomIds) {
        if (symptomIds.isEmpty()) return List.of();
        return symptomDictMapper.selectBatchIds(symptomIds);
    }

    private String saveRuleSymptoms(int ruleId, List<Integer> symptomIds) {
        for (Integer symptomId : symptomIds) {
            if (redFlagRuleSymptomMapper.insert(new RedFlagRuleSymptom(ruleId, symptomId)) <= 0) {
                return "红旗规则关联症状保存失败，请联系管理员";
            }
        }
        return null;
    }

    private boolean existsByCode(String ruleCode, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<RedFlagRule>query()
                .eq("rule_code", ruleCode.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean existsTriageLevel(int triageLevelId) {
        TriageLevelDict level = triageLevelDictMapper.selectById(triageLevelId);
        return level != null;
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
