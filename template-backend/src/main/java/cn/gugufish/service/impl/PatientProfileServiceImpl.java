package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.vo.request.PatientProfileCreateVO;
import cn.gugufish.entity.vo.request.PatientProfileUpdateVO;
import cn.gugufish.mapper.PatientProfileMapper;
import cn.gugufish.service.PatientProfileService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class PatientProfileServiceImpl extends ServiceImpl<PatientProfileMapper, PatientProfile> implements PatientProfileService {

    @Resource
    PatientProfileMapper patientProfileMapper;

    @Override
    public List<PatientProfile> listByAccountId(int accountId) {
        return this.list(Wrappers.<PatientProfile>query()
                .eq("account_id", accountId)
                .orderByDesc("is_default")
                .orderByDesc("is_self")
                .orderByAsc("id"));
    }

    @Override
    @Transactional
    public String createProfile(int accountId, PatientProfileCreateVO vo) {
        boolean isSelf = isSelfRelation(vo.getRelationType());
        if (isSelf && existsSelfProfile(accountId, null)) {
            return "本人就诊人档案已存在，请直接编辑现有档案";
        }

        int defaultValue = resolveDefaultValueForCreate(accountId, vo.getIsDefault());
        if (defaultValue == 1) {
            clearDefaultFlag(accountId, null);
        }

        Date now = new Date();
        PatientProfile profile = new PatientProfile(
                null,
                accountId,
                vo.getName().trim(),
                vo.getGender(),
                vo.getBirthDate(),
                blankToNull(vo.getPhone()),
                blankToNull(vo.getIdCard()),
                vo.getRelationType(),
                isSelf ? 1 : 0,
                defaultValue,
                blankToNull(vo.getRemark()),
                vo.getStatus(),
                now,
                now
        );
        return this.save(profile) ? null : "就诊人新增失败，请联系管理员";
    }

    @Override
    @Transactional
    public String updateProfile(int accountId, PatientProfileUpdateVO vo) {
        PatientProfile current = ownedProfile(accountId, vo.getId());
        if (current == null) return "就诊人不存在";

        boolean isSelf = isSelfRelation(vo.getRelationType());
        if (isSelf && existsSelfProfile(accountId, vo.getId())) {
            return "本人就诊人档案已存在，请勿重复设置";
        }
        if (current.getIsDefault() != null && current.getIsDefault() == 1
                && vo.getIsDefault() != null && vo.getIsDefault() == 0
                && !hasOtherDefaultProfile(accountId, vo.getId())) {
            return "请至少保留一个默认就诊人，可先将其他就诊人设为默认";
        }

        if (vo.getIsDefault() != null && vo.getIsDefault() == 1) {
            clearDefaultFlag(accountId, vo.getId());
        }

        boolean updated = this.update(Wrappers.<PatientProfile>update()
                .eq("id", vo.getId())
                .eq("account_id", accountId)
                .set("name", vo.getName().trim())
                .set("gender", vo.getGender())
                .set("birth_date", vo.getBirthDate())
                .set("phone", blankToNull(vo.getPhone()))
                .set("id_card", blankToNull(vo.getIdCard()))
                .set("relation_type", vo.getRelationType())
                .set("is_self", isSelf ? 1 : 0)
                .set("is_default", vo.getIsDefault())
                .set("remark", blankToNull(vo.getRemark()))
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "就诊人更新失败，请联系管理员";
    }

    @Override
    @Transactional
    public String deleteProfile(int accountId, int id) {
        PatientProfile current = ownedProfile(accountId, id);
        if (current == null) return "就诊人不存在";

        boolean removed = this.remove(Wrappers.<PatientProfile>query()
                .eq("id", id)
                .eq("account_id", accountId));
        if (!removed) return "就诊人删除失败，请联系管理员";

        if (current.getIsDefault() != null && current.getIsDefault() == 1) {
            PatientProfile next = this.getOne(Wrappers.<PatientProfile>query()
                    .eq("account_id", accountId)
                    .orderByDesc("is_self")
                    .orderByAsc("id")
                    .last("limit 1"), false);
            if (next != null) {
                this.update(Wrappers.<PatientProfile>update()
                        .eq("id", next.getId())
                        .set("is_default", 1)
                        .set("update_time", new Date()));
            }
        }
        return null;
    }

    private PatientProfile ownedProfile(int accountId, int id) {
        return this.getOne(Wrappers.<PatientProfile>query()
                .eq("id", id)
                .eq("account_id", accountId), false);
    }

    private boolean existsSelfProfile(int accountId, Integer ignoreId) {
        return patientProfileMapper.exists(Wrappers.<PatientProfile>query()
                .eq("account_id", accountId)
                .eq("is_self", 1)
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean hasOtherDefaultProfile(int accountId, Integer ignoreId) {
        return patientProfileMapper.exists(Wrappers.<PatientProfile>query()
                .eq("account_id", accountId)
                .eq("is_default", 1)
                .ne(ignoreId != null, "id", ignoreId));
    }

    private int resolveDefaultValueForCreate(int accountId, Integer requestedDefault) {
        boolean hasAnyProfile = patientProfileMapper.exists(Wrappers.<PatientProfile>query()
                .eq("account_id", accountId));
        if (!hasAnyProfile) return 1;
        return requestedDefault != null && requestedDefault == 1 ? 1 : 0;
    }

    private void clearDefaultFlag(int accountId, Integer ignoreId) {
        this.update(Wrappers.<PatientProfile>update()
                .eq("account_id", accountId)
                .eq("is_default", 1)
                .ne(ignoreId != null, "id", ignoreId)
                .set("is_default", 0)
                .set("update_time", new Date()));
    }

    private boolean isSelfRelation(String relationType) {
        return "self".equals(relationType);
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
