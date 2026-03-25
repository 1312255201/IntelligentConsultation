package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.RedFlagRule;
import cn.gugufish.entity.dto.TriageLevelDict;
import cn.gugufish.entity.vo.request.TriageLevelDictCreateVO;
import cn.gugufish.entity.vo.request.TriageLevelDictUpdateVO;
import cn.gugufish.mapper.RedFlagRuleMapper;
import cn.gugufish.mapper.TriageLevelDictMapper;
import cn.gugufish.service.TriageLevelDictService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TriageLevelDictServiceImpl extends ServiceImpl<TriageLevelDictMapper, TriageLevelDict> implements TriageLevelDictService {

    @Resource
    RedFlagRuleMapper redFlagRuleMapper;

    @Override
    public List<TriageLevelDict> listTriageLevels() {
        return this.list(Wrappers.<TriageLevelDict>query()
                .orderByDesc("priority")
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createTriageLevel(TriageLevelDictCreateVO vo) {
        if (existsByName(vo.getName(), null)) return "紧急等级名称已存在";
        if (existsByCode(vo.getCode(), null)) return "紧急等级编码已存在";

        Date now = new Date();
        TriageLevelDict level = new TriageLevelDict(
                null,
                vo.getName().trim(),
                vo.getCode().trim(),
                blankToNull(vo.getDescription()),
                blankToNull(vo.getSuggestion()),
                blankToNull(vo.getColor()),
                vo.getPriority(),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(level) ? null : "紧急等级新增失败，请联系管理员";
    }

    @Override
    public String updateTriageLevel(TriageLevelDictUpdateVO vo) {
        TriageLevelDict current = this.getById(vo.getId());
        if (current == null) return "紧急等级不存在";
        if (existsByName(vo.getName(), vo.getId())) return "紧急等级名称已存在";
        if (existsByCode(vo.getCode(), vo.getId())) return "紧急等级编码已存在";

        boolean updated = this.update(Wrappers.<TriageLevelDict>update()
                .eq("id", vo.getId())
                .set("name", vo.getName().trim())
                .set("code", vo.getCode().trim())
                .set("description", blankToNull(vo.getDescription()))
                .set("suggestion", blankToNull(vo.getSuggestion()))
                .set("color", blankToNull(vo.getColor()))
                .set("priority", vo.getPriority())
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "紧急等级更新失败，请联系管理员";
    }

    @Override
    public String deleteTriageLevel(int id) {
        TriageLevelDict current = this.getById(id);
        if (current == null) return "紧急等级不存在";
        if (redFlagRuleMapper.exists(Wrappers.<RedFlagRule>query().eq("triage_level_id", id))) {
            return "当前紧急等级已被红旗规则引用，请先解除规则引用后再删除";
        }
        return this.removeById(id) ? null : "紧急等级删除失败，请联系管理员";
    }

    private boolean existsByName(String name, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<TriageLevelDict>query()
                .eq("name", name.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean existsByCode(String code, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<TriageLevelDict>query()
                .eq("code", code.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
