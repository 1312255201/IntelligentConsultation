package cn.gugufish.service;

import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.vo.request.DepartmentCreateVO;
import cn.gugufish.entity.vo.request.DepartmentUpdateVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    List<Department> listDepartments();
    String createDepartment(DepartmentCreateVO vo);
    String updateDepartment(DepartmentUpdateVO vo);
    String deleteDepartment(int id);
}
