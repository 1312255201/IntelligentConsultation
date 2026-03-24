package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.DoctorServiceTagCreateVO;
import cn.gugufish.entity.vo.request.DoctorServiceTagUpdateVO;
import cn.gugufish.entity.vo.response.DoctorServiceTagVO;
import cn.gugufish.service.DoctorServiceTagService;
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
@RequestMapping("/api/admin/doctor-service-tag")
@Tag(name = "管理员医生服务标签维护", description = "管理员对医生服务能力标签进行维护")
public class AdminDoctorServiceTagController {

    @Resource
    DoctorServiceTagService doctorServiceTagService;

    @GetMapping("/list")
    @Operation(summary = "查询医生服务标签列表")
    public RestBean<List<DoctorServiceTagVO>> list() {
        return RestBean.success(doctorServiceTagService.listDoctorServiceTags().stream()
                .map(item -> item.asViewObject(DoctorServiceTagVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增医生服务标签")
    public RestBean<Void> create(@RequestBody @Valid DoctorServiceTagCreateVO vo) {
        return this.messageHandle(() -> doctorServiceTagService.createDoctorServiceTag(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新医生服务标签")
    public RestBean<Void> update(@RequestBody @Valid DoctorServiceTagUpdateVO vo) {
        return this.messageHandle(() -> doctorServiceTagService.updateDoctorServiceTag(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除医生服务标签")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> doctorServiceTagService.deleteDoctorServiceTag(id));
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
