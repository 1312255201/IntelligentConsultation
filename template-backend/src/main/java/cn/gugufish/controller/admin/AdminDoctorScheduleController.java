package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.DoctorScheduleCreateVO;
import cn.gugufish.entity.vo.request.DoctorScheduleUpdateVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.service.DoctorScheduleService;
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
@RequestMapping("/api/admin/doctor-schedule")
@Tag(name = "管理员医生排班维护", description = "管理员对医生排班信息进行维护")
public class AdminDoctorScheduleController {

    @Resource
    DoctorScheduleService doctorScheduleService;

    @GetMapping("/list")
    @Operation(summary = "查询医生排班列表")
    public RestBean<List<DoctorScheduleVO>> list() {
        return RestBean.success(doctorScheduleService.listDoctorSchedules().stream()
                .map(item -> item.asViewObject(DoctorScheduleVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增医生排班")
    public RestBean<Void> create(@RequestBody @Valid DoctorScheduleCreateVO vo) {
        return this.messageHandle(() -> doctorScheduleService.createDoctorSchedule(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新医生排班")
    public RestBean<Void> update(@RequestBody @Valid DoctorScheduleUpdateVO vo) {
        return this.messageHandle(() -> doctorScheduleService.updateDoctorSchedule(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除医生排班")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> doctorScheduleService.deleteDoctorSchedule(id));
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
