package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.DepartmentCreateVO;
import cn.gugufish.entity.vo.request.DepartmentUpdateVO;
import cn.gugufish.entity.vo.response.DepartmentVO;
import cn.gugufish.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/admin/department")
@Tag(name = "管理员科室维护", description = "管理员对科室信息进行新增、修改、删除和查询")
public class AdminDepartmentController {

    @Resource
    DepartmentService departmentService;

    @GetMapping("/list")
    @Operation(summary = "查询科室列表")
    public RestBean<List<DepartmentVO>> list() {
        return RestBean.success(departmentService.listDepartments()
                .stream()
                .map(item -> item.asViewObject(DepartmentVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增科室")
    public RestBean<Void> create(@RequestBody @Valid DepartmentCreateVO vo) {
        return this.messageHandle(() -> departmentService.createDepartment(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新科室")
    public RestBean<Void> update(@RequestBody @Valid DepartmentUpdateVO vo) {
        return this.messageHandle(() -> departmentService.updateDepartment(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除科室")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> departmentService.deleteDepartment(id));
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        if (message == null) {
            return RestBean.success();
        } else {
            return RestBean.failure(400, message);
        }
    }
}
