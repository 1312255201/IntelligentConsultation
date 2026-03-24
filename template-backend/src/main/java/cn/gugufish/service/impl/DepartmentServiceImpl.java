package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.vo.request.DepartmentCreateVO;
import cn.gugufish.entity.vo.request.DepartmentUpdateVO;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.service.DepartmentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Resource
    DoctorMapper doctorMapper;

    @Override
    public List<Department> listDepartments() {
        return this.list(Wrappers.<Department>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createDepartment(DepartmentCreateVO vo) {
        if (existsByName(vo.getName(), null)) return "科室名称已存在";
        if (existsByCode(vo.getCode(), null)) return "科室编码已存在";

        Date now = new Date();
        Department department = new Department(
                null,
                vo.getName(),
                vo.getCode(),
                vo.getDescription(),
                vo.getLocation(),
                vo.getPhone(),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(department) ? null : "科室新增失败，请联系管理员";
    }

    @Override
    public String updateDepartment(DepartmentUpdateVO vo) {
        Department department = this.getById(vo.getId());
        if (department == null) return "科室不存在";
        if (existsByName(vo.getName(), vo.getId())) return "科室名称已存在";
        if (existsByCode(vo.getCode(), vo.getId())) return "科室编码已存在";

        boolean updated = this.update(Wrappers.<Department>update()
                .eq("id", vo.getId())
                .set("name", vo.getName())
                .set("code", vo.getCode())
                .set("description", vo.getDescription())
                .set("location", vo.getLocation())
                .set("phone", vo.getPhone())
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "科室更新失败，请联系管理员";
    }

    @Override
    public String deleteDepartment(int id) {
        Department department = this.getById(id);
        if (department == null) return "科室不存在";
        if (doctorMapper.exists(Wrappers.<Doctor>query().eq("department_id", id))) {
            return "当前科室下已有绑定医生，暂时不能删除";
        }
        return this.removeById(id) ? null : "科室删除失败，请联系管理员";
    }

    private boolean existsByName(String name, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<Department>query()
                .eq("name", name)
                .ne(ignoreId != null, "id", ignoreId));
    }

    private boolean existsByCode(String code, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<Department>query()
                .eq("code", code)
                .ne(ignoreId != null, "id", ignoreId));
    }
}
