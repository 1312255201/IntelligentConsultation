package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.BodyPartDict;
import cn.gugufish.entity.dto.RedFlagRuleSymptom;
import cn.gugufish.entity.dto.SymptomDict;
import cn.gugufish.entity.vo.request.SymptomDictCreateVO;
import cn.gugufish.entity.vo.request.SymptomDictUpdateVO;
import cn.gugufish.mapper.BodyPartDictMapper;
import cn.gugufish.mapper.RedFlagRuleSymptomMapper;
import cn.gugufish.mapper.SymptomDictMapper;
import cn.gugufish.service.SymptomDictService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SymptomDictServiceImpl extends ServiceImpl<SymptomDictMapper, SymptomDict> implements SymptomDictService {

    @Resource
    BodyPartDictMapper bodyPartDictMapper;

    @Resource
    RedFlagRuleSymptomMapper redFlagRuleSymptomMapper;

    @Override
    public List<SymptomDict> listSymptoms() {
        return this.list(Wrappers.<SymptomDict>query()
                .orderByAsc("body_part_id")
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createSymptom(SymptomDictCreateVO vo) {
        if (!existsBodyPart(vo.getBodyPartId())) return "关联部位不存在";
        if (existsByCode(vo.getCode(), null)) return "症状编码已存在";
        if (existsByName(vo.getBodyPartId(), vo.getName(), null)) return "同一部位下的症状名称已存在";

        Date now = new Date();
        SymptomDict symptom = new SymptomDict(
                null,
                vo.getBodyPartId(),
                vo.getName().trim(),
                vo.getCode().trim(),
                blankToNull(vo.getKeywords()),
                blankToNull(vo.getAliasKeywords()),
                blankToNull(vo.getDescription()),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(symptom) ? null : "症状字典新增失败，请联系管理员";
    }

    @Override
    public String updateSymptom(SymptomDictUpdateVO vo) {
        SymptomDict current = this.getById(vo.getId());
        if (current == null) return "症状字典不存在";
        if (!existsBodyPart(vo.getBodyPartId())) return "关联部位不存在";
        if (existsByCode(vo.getCode(), vo.getId())) return "症状编码已存在";
        if (existsByName(vo.getBodyPartId(), vo.getName(), vo.getId())) return "同一部位下的症状名称已存在";

        boolean updated = this.update(Wrappers.<SymptomDict>update()
                .eq("id", vo.getId())
                .set("body_part_id", vo.getBodyPartId())
                .set("name", vo.getName().trim())
                .set("code", vo.getCode().trim())
                .set("keywords", blankToNull(vo.getKeywords()))
                .set("alias_keywords", blankToNull(vo.getAliasKeywords()))
                .set("description", blankToNull(vo.getDescription()))
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "症状字典更新失败，请联系管理员";
    }

    @Override
    public String deleteSymptom(int id) {
        SymptomDict current = this.getById(id);
        if (current == null) return "症状字典不存在";
        if (redFlagRuleSymptomMapper.exists(Wrappers.<RedFlagRuleSymptom>query().eq("symptom_id", id))) {
            return "当前症状已被红旗规则引用，请先解除规则引用后再删除";
        }
        return this.removeById(id) ? null : "症状字典删除失败，请联系管理员";
    }

    private boolean existsBodyPart(int bodyPartId) {
        BodyPartDict bodyPart = bodyPartDictMapper.selectById(bodyPartId);
        return bodyPart != null;
    }

    private boolean existsByCode(String code, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<SymptomDict>query()
                .eq("code", code.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean existsByName(int bodyPartId, String name, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<SymptomDict>query()
                .eq("body_part_id", bodyPartId)
                .eq("name", name.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
