package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.MedicineCreateVO;
import cn.gugufish.entity.vo.request.MedicineUpdateVO;
import cn.gugufish.entity.vo.response.MedicineCatalogVO;
import cn.gugufish.service.MedicineCatalogService;
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
@RequestMapping("/api/admin/medicine")
@Tag(name = "管理员药品目录", description = "管理员维护药品基础信息、禁忌和联用互斥规则")
public class AdminMedicineController {

    @Resource
    MedicineCatalogService medicineCatalogService;

    @GetMapping("/list")
    @Operation(summary = "查询药品目录")
    public RestBean<List<MedicineCatalogVO>> list() {
        return RestBean.success(medicineCatalogService.listMedicines(false));
    }

    @PostMapping("/create")
    @Operation(summary = "新增药品")
    public RestBean<Void> create(@RequestBody @Valid MedicineCreateVO vo) {
        return this.messageHandle(() -> medicineCatalogService.createMedicine(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新药品")
    public RestBean<Void> update(@RequestBody @Valid MedicineUpdateVO vo) {
        return this.messageHandle(() -> medicineCatalogService.updateMedicine(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除药品")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> medicineCatalogService.deleteMedicine(id));
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
