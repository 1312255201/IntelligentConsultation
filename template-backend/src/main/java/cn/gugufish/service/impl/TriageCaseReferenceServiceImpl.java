package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.TriageCaseReference;
import cn.gugufish.entity.vo.request.TriageCaseReferenceCreateVO;
import cn.gugufish.entity.vo.request.TriageCaseReferenceUpdateVO;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.TriageCaseReferenceMapper;
import cn.gugufish.service.TriageCaseReferenceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TriageCaseReferenceServiceImpl extends ServiceImpl<TriageCaseReferenceMapper, TriageCaseReference> implements TriageCaseReferenceService {

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    DoctorMapper doctorMapper;

    @Override
    public List<TriageCaseReference> listCaseReferences() {
        return this.list(Wrappers.<TriageCaseReference>query()
                .orderByAsc("sort")
                .orderByDesc("update_time")
                .orderByDesc("id"));
    }

    @Override
    public String createCaseReference(TriageCaseReferenceCreateVO vo) {
        String validate = validateReference(vo.getDepartmentId(), vo.getDoctorId());
        if (validate != null) return validate;
        if (existsByTitle(vo.getTitle(), null)) return "导诊案例标题已存在";

        Date now = new Date();
        TriageCaseReference entity = new TriageCaseReference(
                null,
                vo.getTitle().trim(),
                vo.getChiefComplaint().trim(),
                blankToNull(vo.getSymptomSummary()),
                vo.getTriageResult().trim(),
                vo.getDepartmentId(),
                vo.getDoctorId(),
                vo.getRiskLevel().trim(),
                blankToNull(vo.getTags()),
                vo.getSourceType().trim(),
                defaultSort(vo.getSort()),
                defaultStatus(vo.getStatus()),
                now,
                now
        );
        return this.save(entity) ? null : "导诊案例新增失败，请联系管理员";
    }

    @Override
    public String updateCaseReference(TriageCaseReferenceUpdateVO vo) {
        TriageCaseReference current = this.getById(vo.getId());
        if (current == null) return "导诊案例不存在";

        String validate = validateReference(vo.getDepartmentId(), vo.getDoctorId());
        if (validate != null) return validate;
        if (existsByTitle(vo.getTitle(), vo.getId())) return "导诊案例标题已存在";

        boolean updated = this.update(Wrappers.<TriageCaseReference>update()
                .eq("id", vo.getId())
                .set("title", vo.getTitle().trim())
                .set("chief_complaint", vo.getChiefComplaint().trim())
                .set("symptom_summary", blankToNull(vo.getSymptomSummary()))
                .set("triage_result", vo.getTriageResult().trim())
                .set("department_id", vo.getDepartmentId())
                .set("doctor_id", vo.getDoctorId())
                .set("risk_level", vo.getRiskLevel().trim())
                .set("tags", blankToNull(vo.getTags()))
                .set("source_type", vo.getSourceType().trim())
                .set("sort", defaultSort(vo.getSort()))
                .set("status", defaultStatus(vo.getStatus()))
                .set("update_time", new Date()));
        return updated ? null : "导诊案例更新失败，请联系管理员";
    }

    @Override
    public String deleteCaseReference(int id) {
        TriageCaseReference current = this.getById(id);
        if (current == null) return "导诊案例不存在";
        return this.removeById(id) ? null : "导诊案例删除失败，请联系管理员";
    }

    private boolean existsByTitle(String title, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<TriageCaseReference>query()
                .eq("title", title.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String validateReference(Integer departmentId, Integer doctorId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department == null) return "关联的科室不存在";
        if (doctorId != null) {
            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor == null) return "关联的医生不存在";
            if (doctor.getDepartmentId() == null || !doctor.getDepartmentId().equals(departmentId)) {
                return "所选医生与科室不匹配";
            }
        }
        return null;
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
}
