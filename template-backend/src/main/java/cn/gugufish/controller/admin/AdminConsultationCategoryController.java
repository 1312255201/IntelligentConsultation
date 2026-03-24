package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ConsultationCategoryCreateVO;
import cn.gugufish.entity.vo.request.ConsultationCategoryUpdateVO;
import cn.gugufish.entity.vo.response.ConsultationCategoryVO;
import cn.gugufish.service.ConsultationCategoryService;
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
@RequestMapping("/api/admin/consultation-category")
@Tag(name = "管理员问诊分类维护", description = "管理员对问诊入口分类进行维护")
public class AdminConsultationCategoryController {

    @Resource
    ConsultationCategoryService consultationCategoryService;

    @GetMapping("/list")
    @Operation(summary = "查询问诊分类列表")
    public RestBean<List<ConsultationCategoryVO>> list() {
        return RestBean.success(consultationCategoryService.listCategories()
                .stream()
                .map(item -> item.asViewObject(ConsultationCategoryVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增问诊分类")
    public RestBean<Void> create(@RequestBody @Valid ConsultationCategoryCreateVO vo) {
        return this.messageHandle(() -> consultationCategoryService.createCategory(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新问诊分类")
    public RestBean<Void> update(@RequestBody @Valid ConsultationCategoryUpdateVO vo) {
        return this.messageHandle(() -> consultationCategoryService.updateCategory(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除问诊分类")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> consultationCategoryService.deleteCategory(id));
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
