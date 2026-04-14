package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationCategory;
import cn.gugufish.entity.dto.ConsultationIntakeTemplate;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.vo.request.ConsultationCategoryCreateVO;
import cn.gugufish.entity.vo.request.ConsultationCategoryUpdateVO;
import cn.gugufish.mapper.ConsultationCategoryMapper;
import cn.gugufish.mapper.ConsultationIntakeTemplateMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.service.ConsultationCategoryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class ConsultationCategoryServiceImpl extends ServiceImpl<ConsultationCategoryMapper, ConsultationCategory> implements ConsultationCategoryService {

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    ConsultationIntakeTemplateMapper intakeTemplateMapper;

    @Override
    public List<ConsultationCategory> listCategories() {
        return this.list(Wrappers.<ConsultationCategory>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createCategory(ConsultationCategoryCreateVO vo) {
        if (!existsDepartment(vo.getDepartmentId())) return "关联科室不存在";
        if (existsByName(vo.getName(), null)) return "问诊分类名称已存在";
        if (existsByCode(vo.getCode(), null)) return "问诊分类编码已存在";

        Date now = new Date();
        ConsultationCategory category = new ConsultationCategory(
                null,
                vo.getDepartmentId(),
                vo.getName().trim(),
                vo.getCode().trim(),
                blankToNull(vo.getDescription()),
                normalizeAmount(vo.getPriceAmount()),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(category) ? null : "问诊分类新增失败，请联系管理员";
    }

    @Override
    public String updateCategory(ConsultationCategoryUpdateVO vo) {
        ConsultationCategory current = this.getById(vo.getId());
        if (current == null) return "问诊分类不存在";
        if (!existsDepartment(vo.getDepartmentId())) return "关联科室不存在";
        if (existsByName(vo.getName(), vo.getId())) return "问诊分类名称已存在";
        if (existsByCode(vo.getCode(), vo.getId())) return "问诊分类编码已存在";

        boolean updated = this.update(Wrappers.<ConsultationCategory>update()
                .eq("id", vo.getId())
                .set("department_id", vo.getDepartmentId())
                .set("name", vo.getName().trim())
                .set("code", vo.getCode().trim())
                .set("description", blankToNull(vo.getDescription()))
                .set("price_amount", normalizeAmount(vo.getPriceAmount()))
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "问诊分类更新失败，请联系管理员";
    }

    @Override
    public String deleteCategory(int id) {
        ConsultationCategory current = this.getById(id);
        if (current == null) return "问诊分类不存在";
        if (intakeTemplateMapper.exists(Wrappers.<ConsultationIntakeTemplate>query().eq("category_id", id))) {
            return "当前问诊分类下仍有关联的前置模板，请先删除模板后再删除分类";
        }
        return this.removeById(id) ? null : "问诊分类删除失败，请联系管理员";
    }

    private boolean existsDepartment(int departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        return department != null;
    }

    private boolean existsByName(String name, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<ConsultationCategory>query()
                .eq("name", name.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean existsByCode(String code, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<ConsultationCategory>query()
                .eq("code", code.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.compareTo(BigDecimal.ZERO) < 0) value = BigDecimal.ZERO;
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
