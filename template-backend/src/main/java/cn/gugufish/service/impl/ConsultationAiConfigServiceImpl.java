package cn.gugufish.service.impl;

import cn.gugufish.ai.AiTriageProperties;
import cn.gugufish.entity.dto.Account;
import cn.gugufish.entity.dto.ConsultationAiConfig;
import cn.gugufish.entity.dto.ConsultationAiConfigHistory;
import cn.gugufish.entity.vo.request.ConsultationAiConfigSaveVO;
import cn.gugufish.entity.vo.response.ConsultationAiConfigHistoryVO;
import cn.gugufish.mapper.AccountMapper;
import cn.gugufish.mapper.ConsultationAiConfigMapper;
import cn.gugufish.mapper.ConsultationAiConfigHistoryMapper;
import cn.gugufish.service.ConsultationAiConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ConsultationAiConfigServiceImpl implements ConsultationAiConfigService {

    private static final int CONFIG_ID = 1;

    @Resource
    ConsultationAiConfigMapper consultationAiConfigMapper;

    @Resource
    ConsultationAiConfigHistoryMapper consultationAiConfigHistoryMapper;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AiTriageProperties aiTriageProperties;

    @PostConstruct
    public void initRuntimeConfig() {
        try {
            applyToRuntime(getConfig());
        } catch (Exception exception) {
            log.warn("Failed to initialize consultation AI runtime config, fallback to environment defaults: {}",
                    exception.getMessage());
        }
    }

    @Override
    public ConsultationAiConfig getConfig() {
        ConsultationAiConfig config = consultationAiConfigMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = defaultConfig(new Date());
            consultationAiConfigMapper.insert(config);
        } else {
            config = normalizeConfig(config);
        }
        applyToRuntime(config);
        return config;
    }

    @Override
    public String saveConfig(int operatorAccountId, ConsultationAiConfigSaveVO vo) {
        ConsultationAiConfig current = getConfig();
        ConsultationAiConfig config = new ConsultationAiConfig(
                CONFIG_ID,
                vo.getEnabled(),
                normalizePromptVersion(vo.getPromptVersion()),
                vo.getDoctorCandidateLimit(),
                new Date()
        );
        if (sameConfig(current, config)) {
            applyToRuntime(current);
            return null;
        }
        boolean success = consultationAiConfigMapper.selectById(CONFIG_ID) == null
                ? consultationAiConfigMapper.insert(config) > 0
                : consultationAiConfigMapper.updateById(config) > 0;
        if (!success) {
            return "AI 配置保存失败，请稍后重试";
        }
        insertHistory(current, config, operatorAccountId);
        applyToRuntime(config);
        return null;
    }

    @Override
    public List<ConsultationAiConfigHistoryVO> listHistory(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 30));
        return consultationAiConfigHistoryMapper.selectList(Wrappers.<ConsultationAiConfigHistory>query()
                        .orderByDesc("id")
                        .last("limit " + safeLimit))
                .stream()
                .map(item -> item.asViewObject(ConsultationAiConfigHistoryVO.class))
                .toList();
    }

    private ConsultationAiConfig normalizeConfig(ConsultationAiConfig config) {
        ConsultationAiConfig fallback = defaultConfig(new Date());
        boolean changed = false;

        if (config.getEnabled() == null || (config.getEnabled() != 0 && config.getEnabled() != 1)) {
            config.setEnabled(fallback.getEnabled());
            changed = true;
        }
        if (!StringUtils.hasText(config.getPromptVersion())) {
            config.setPromptVersion(fallback.getPromptVersion());
            changed = true;
        } else {
            String normalizedPromptVersion = config.getPromptVersion().trim();
            if (!normalizedPromptVersion.equals(config.getPromptVersion())) {
                config.setPromptVersion(normalizedPromptVersion);
                changed = true;
            }
        }
        if (config.getDoctorCandidateLimit() == null || config.getDoctorCandidateLimit() <= 0) {
            config.setDoctorCandidateLimit(fallback.getDoctorCandidateLimit());
            changed = true;
        }
        if (changed) {
            config.setUpdateTime(new Date());
            consultationAiConfigMapper.updateById(config);
        }
        return config;
    }

    private ConsultationAiConfig defaultConfig(Date updateTime) {
        return new ConsultationAiConfig(
                CONFIG_ID,
                aiTriageProperties.isDefaultEnabled() ? 1 : 0,
                normalizePromptVersion(aiTriageProperties.getDefaultPromptVersion()),
                Math.max(aiTriageProperties.getDefaultDoctorCandidateLimit(), 1),
                updateTime
        );
    }

    private void applyToRuntime(ConsultationAiConfig config) {
        if (config == null) return;
        aiTriageProperties.setEnabled(config.getEnabled() == null || config.getEnabled() != 0);
        aiTriageProperties.setPromptVersion(normalizePromptVersion(config.getPromptVersion()));
        aiTriageProperties.setDoctorCandidateLimit(Math.max(config.getDoctorCandidateLimit() == null ? 1 : config.getDoctorCandidateLimit(), 1));
    }

    private String normalizePromptVersion(String promptVersion) {
        if (StringUtils.hasText(promptVersion)) {
            return promptVersion.trim();
        }
        String fallback = aiTriageProperties.getDefaultPromptVersion();
        return StringUtils.hasText(fallback) ? fallback.trim() : "deepseek-triage-v1";
    }

    private boolean sameConfig(ConsultationAiConfig current, ConsultationAiConfig next) {
        if (current == null || next == null) return false;
        return current.getEnabled().equals(next.getEnabled())
                && current.getDoctorCandidateLimit().equals(next.getDoctorCandidateLimit())
                && normalizePromptVersion(current.getPromptVersion()).equals(normalizePromptVersion(next.getPromptVersion()));
    }

    private void insertHistory(ConsultationAiConfig before, ConsultationAiConfig after, int operatorAccountId) {
        String operatorUsername = resolveOperatorUsername(operatorAccountId);
        ConsultationAiConfigHistory history = new ConsultationAiConfigHistory(
                null,
                CONFIG_ID,
                before == null ? null : before.getEnabled(),
                after.getEnabled(),
                before == null ? null : normalizePromptVersion(before.getPromptVersion()),
                normalizePromptVersion(after.getPromptVersion()),
                before == null ? null : before.getDoctorCandidateLimit(),
                after.getDoctorCandidateLimit(),
                operatorAccountId > 0 ? operatorAccountId : null,
                operatorUsername,
                buildChangeSummary(before, after),
                new Date()
        );
        consultationAiConfigHistoryMapper.insert(history);
    }

    private String resolveOperatorUsername(int operatorAccountId) {
        if (operatorAccountId <= 0) return "system";
        Account account = accountMapper.selectById(operatorAccountId);
        if (account == null || !StringUtils.hasText(account.getUsername())) {
            return "admin#" + operatorAccountId;
        }
        return account.getUsername();
    }

    private String buildChangeSummary(ConsultationAiConfig before, ConsultationAiConfig after) {
        List<String> changes = new ArrayList<>();
        Integer beforeEnabled = before == null ? null : before.getEnabled();
        if (beforeEnabled == null || !beforeEnabled.equals(after.getEnabled())) {
            changes.add("AI导诊开关：" + enabledLabel(beforeEnabled) + " -> " + enabledLabel(after.getEnabled()));
        }
        String beforePromptVersion = before == null ? null : normalizePromptVersion(before.getPromptVersion());
        String afterPromptVersion = normalizePromptVersion(after.getPromptVersion());
        if (beforePromptVersion == null || !beforePromptVersion.equals(afterPromptVersion)) {
            changes.add("Prompt版本：" + safeText(beforePromptVersion) + " -> " + safeText(afterPromptVersion));
        }
        Integer beforeLimit = before == null ? null : before.getDoctorCandidateLimit();
        if (beforeLimit == null || !beforeLimit.equals(after.getDoctorCandidateLimit())) {
            changes.add("候选医生上限：" + safeText(beforeLimit) + " -> " + safeText(after.getDoctorCandidateLimit()));
        }
        return changes.isEmpty() ? "未发生配置变化" : String.join("；", changes);
    }

    private String enabledLabel(Integer value) {
        if (value == null) return "未设置";
        return value == 0 ? "关闭" : "开启";
    }

    private String safeText(Object value) {
        return value == null ? "未设置" : String.valueOf(value);
    }
}
