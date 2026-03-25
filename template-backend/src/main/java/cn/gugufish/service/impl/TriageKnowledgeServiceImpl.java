package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.TriageKnowledge;
import cn.gugufish.entity.vo.request.TriageKnowledgeCreateVO;
import cn.gugufish.entity.vo.request.TriageKnowledgeUpdateVO;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.TriageKnowledgeMapper;
import cn.gugufish.service.TriageKnowledgeService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TriageKnowledgeServiceImpl extends ServiceImpl<TriageKnowledgeMapper, TriageKnowledge> implements TriageKnowledgeService {

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    DoctorMapper doctorMapper;

    @Override
    public List<TriageKnowledge> listKnowledgeItems() {
        return this.list(Wrappers.<TriageKnowledge>query()
                .orderByAsc("sort")
                .orderByDesc("update_time")
                .orderByDesc("id"));
    }

    @Override
    public String createKnowledge(TriageKnowledgeCreateVO vo) {
        ReferenceResult reference = validateReference(vo.getDepartmentId(), vo.getDoctorId());
        if (reference.message != null) return reference.message;
        if (existsByTypeAndTitle(vo.getKnowledgeType(), vo.getTitle(), null)) {
            return "同类型下的知识标题已存在";
        }

        Date now = new Date();
        TriageKnowledge knowledge = new TriageKnowledge(
                null,
                vo.getKnowledgeType().trim(),
                vo.getTitle().trim(),
                vo.getContent().trim(),
                blankToNull(vo.getTags()),
                reference.departmentId,
                reference.doctorId,
                vo.getSourceType().trim(),
                defaultVersion(vo.getVersion()),
                defaultSort(vo.getSort()),
                defaultStatus(vo.getStatus()),
                now,
                now
        );
        return this.save(knowledge) ? null : "导诊知识新增失败，请联系管理员";
    }

    @Override
    public String updateKnowledge(TriageKnowledgeUpdateVO vo) {
        TriageKnowledge current = this.getById(vo.getId());
        if (current == null) return "导诊知识不存在";

        ReferenceResult reference = validateReference(vo.getDepartmentId(), vo.getDoctorId());
        if (reference.message != null) return reference.message;
        if (existsByTypeAndTitle(vo.getKnowledgeType(), vo.getTitle(), vo.getId())) {
            return "同类型下的知识标题已存在";
        }

        boolean updated = this.update(Wrappers.<TriageKnowledge>update()
                .eq("id", vo.getId())
                .set("knowledge_type", vo.getKnowledgeType().trim())
                .set("title", vo.getTitle().trim())
                .set("content", vo.getContent().trim())
                .set("tags", blankToNull(vo.getTags()))
                .set("department_id", reference.departmentId)
                .set("doctor_id", reference.doctorId)
                .set("source_type", vo.getSourceType().trim())
                .set("version", defaultVersion(vo.getVersion()))
                .set("sort", defaultSort(vo.getSort()))
                .set("status", defaultStatus(vo.getStatus()))
                .set("update_time", new Date()));
        return updated ? null : "导诊知识更新失败，请联系管理员";
    }

    @Override
    public String deleteKnowledge(int id) {
        TriageKnowledge current = this.getById(id);
        if (current == null) return "导诊知识不存在";
        return this.removeById(id) ? null : "导诊知识删除失败，请联系管理员";
    }

    private boolean existsByTypeAndTitle(String knowledgeType, String title, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<TriageKnowledge>query()
                .eq("knowledge_type", knowledgeType.trim())
                .eq("title", title.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private ReferenceResult validateReference(Integer departmentId, Integer doctorId) {
        Integer resolvedDepartmentId = departmentId;
        Integer resolvedDoctorId = doctorId;

        if (resolvedDepartmentId != null) {
            Department department = departmentMapper.selectById(resolvedDepartmentId);
            if (department == null) {
                return new ReferenceResult("关联的科室不存在", null, null);
            }
        }

        if (resolvedDoctorId != null) {
            Doctor doctor = doctorMapper.selectById(resolvedDoctorId);
            if (doctor == null) {
                return new ReferenceResult("关联的医生不存在", null, null);
            }
            if (doctor.getDepartmentId() == null) {
                return new ReferenceResult("关联的医生尚未绑定科室", null, null);
            }
            if (resolvedDepartmentId == null) {
                resolvedDepartmentId = doctor.getDepartmentId();
            } else if (!doctor.getDepartmentId().equals(resolvedDepartmentId)) {
                return new ReferenceResult("所选医生与科室不匹配", null, null);
            }
        }

        return new ReferenceResult(null, resolvedDepartmentId, resolvedDoctorId);
    }

    private Integer defaultVersion(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private Integer defaultSort(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private Integer defaultStatus(Integer value) {
        return value == null || value < 0 ? 1 : value;
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private record ReferenceResult(String message, Integer departmentId, Integer doctorId) {
    }
}
