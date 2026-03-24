package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.PatientProfileCreateVO;
import cn.gugufish.entity.vo.request.PatientProfileUpdateVO;
import cn.gugufish.entity.vo.response.PatientProfileVO;
import cn.gugufish.service.PatientProfileService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/user/patient")
@Tag(name = "用户就诊人管理", description = "当前登录用户维护本人及家庭成员就诊人档案")
public class PatientProfileController {

    @Resource
    PatientProfileService patientProfileService;

    @GetMapping("/list")
    @Operation(summary = "查询当前账号的就诊人列表")
    public RestBean<List<PatientProfileVO>> list(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(patientProfileService.listByAccountId(id).stream()
                .map(item -> item.asViewObject(PatientProfileVO.class, vo -> vo.setAge(calculateAge(item.getBirthDate()))))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增就诊人")
    public RestBean<Void> create(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                 @RequestBody @Valid PatientProfileCreateVO vo) {
        return this.messageHandle(() -> patientProfileService.createProfile(id, vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新就诊人")
    public RestBean<Void> update(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                 @RequestBody @Valid PatientProfileUpdateVO vo) {
        return this.messageHandle(() -> patientProfileService.updateProfile(id, vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除就诊人")
    public RestBean<Void> delete(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                 @RequestParam @Positive int patientId) {
        return this.messageHandle(() -> patientProfileService.deleteProfile(id, patientId));
    }

    private Integer calculateAge(java.util.Date birthDate) {
        if (birthDate == null) return null;
        LocalDate date = Instant.ofEpochMilli(birthDate.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        long years = ChronoUnit.YEARS.between(date, LocalDate.now());
        return (int) Math.max(years, 0);
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
