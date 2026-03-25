package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.BodyPartDict;
import cn.gugufish.entity.dto.RedFlagRule;
import cn.gugufish.entity.dto.SymptomDict;
import cn.gugufish.entity.vo.request.BodyPartDictCreateVO;
import cn.gugufish.entity.vo.request.BodyPartDictUpdateVO;
import cn.gugufish.mapper.BodyPartDictMapper;
import cn.gugufish.mapper.RedFlagRuleMapper;
import cn.gugufish.mapper.SymptomDictMapper;
import cn.gugufish.service.BodyPartDictService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BodyPartDictServiceImpl extends ServiceImpl<BodyPartDictMapper, BodyPartDict> implements BodyPartDictService {

    @Resource
    SymptomDictMapper symptomDictMapper;

    @Resource
    RedFlagRuleMapper redFlagRuleMapper;

    @Override
    public List<BodyPartDict> listBodyParts() {
        return this.list(Wrappers.<BodyPartDict>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createBodyPart(BodyPartDictCreateVO vo) {
        String parentMessage = validateParent(vo.getParentId(), null);
        if (parentMessage != null) return parentMessage;
        if (existsByName(vo.getName(), null)) return "部位名称已存在";
        if (existsByCode(vo.getCode(), null)) return "部位编码已存在";

        Date now = new Date();
        BodyPartDict bodyPart = new BodyPartDict(
                null,
                vo.getName().trim(),
                vo.getCode().trim(),
                vo.getParentId(),
                blankToNull(vo.getDescription()),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(bodyPart) ? null : "身体部位新增失败，请联系管理员";
    }

    @Override
    public String updateBodyPart(BodyPartDictUpdateVO vo) {
        BodyPartDict current = this.getById(vo.getId());
        if (current == null) return "身体部位不存在";

        String parentMessage = validateParent(vo.getParentId(), vo.getId());
        if (parentMessage != null) return parentMessage;
        if (existsByName(vo.getName(), vo.getId())) return "部位名称已存在";
        if (existsByCode(vo.getCode(), vo.getId())) return "部位编码已存在";

        boolean updated = this.update(Wrappers.<BodyPartDict>update()
                .eq("id", vo.getId())
                .set("name", vo.getName().trim())
                .set("code", vo.getCode().trim())
                .set("parent_id", vo.getParentId())
                .set("description", blankToNull(vo.getDescription()))
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "身体部位更新失败，请联系管理员";
    }

    @Override
    public String deleteBodyPart(int id) {
        BodyPartDict current = this.getById(id);
        if (current == null) return "身体部位不存在";
        if (this.baseMapper.exists(Wrappers.<BodyPartDict>query().eq("parent_id", id))) {
            return "当前部位下仍存在子部位，请先处理子节点后再删除";
        }
        if (symptomDictMapper.exists(Wrappers.<SymptomDict>query().eq("body_part_id", id))) {
            return "当前部位下仍存在症状字典，请先删除症状后再删除";
        }
        if (redFlagRuleMapper.exists(Wrappers.<RedFlagRule>query().eq("body_part_id", id))) {
            return "当前部位已被红旗规则引用，请先解除规则引用后再删除";
        }
        return this.removeById(id) ? null : "身体部位删除失败，请联系管理员";
    }

    private String validateParent(Integer parentId, Integer selfId) {
        if (parentId == null) return null;
        if (selfId != null && parentId.equals(selfId)) return "上级部位不能选择自己";

        BodyPartDict parent = this.getById(parentId);
        if (parent == null) return "上级部位不存在";

        if (selfId != null) {
            BodyPartDict node = parent;
            while (node != null && node.getParentId() != null) {
                if (node.getParentId().equals(selfId)) {
                    return "上级部位不能选择当前节点的下级节点";
                }
                node = this.getById(node.getParentId());
            }
        }
        return null;
    }

    private boolean existsByName(String name, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<BodyPartDict>query()
                .eq("name", name.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean existsByCode(String code, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<BodyPartDict>query()
                .eq("code", code.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
