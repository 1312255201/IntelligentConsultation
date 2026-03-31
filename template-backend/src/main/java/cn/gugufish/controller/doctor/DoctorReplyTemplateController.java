package cn.gugufish.controller.doctor;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.DoctorReplyTemplateCreateVO;
import cn.gugufish.entity.vo.request.DoctorReplyTemplateUpdateVO;
import cn.gugufish.entity.vo.response.DoctorReplyTemplateVO;
import cn.gugufish.service.DoctorReplyTemplateService;
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

import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/doctor/reply-template")
@Tag(name = "医生常用回复模板", description = "医生维护个人常用回复模板")
public class DoctorReplyTemplateController {

    @Resource
    DoctorReplyTemplateService doctorReplyTemplateService;

    @GetMapping("/list")
    @Operation(summary = "查询当前医生的常用回复模板列表")
    public RestBean<List<DoctorReplyTemplateVO>> list(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(doctorReplyTemplateService.listDoctorReplyTemplates(accountId).stream()
                .map(item -> item.asViewObject(DoctorReplyTemplateVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增常用回复模板")
    public RestBean<Void> create(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                 @RequestBody @Valid DoctorReplyTemplateCreateVO vo) {
        return this.messageHandle(() -> doctorReplyTemplateService.createDoctorReplyTemplate(accountId, vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新常用回复模板")
    public RestBean<Void> update(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                 @RequestBody @Valid DoctorReplyTemplateUpdateVO vo) {
        return this.messageHandle(() -> doctorReplyTemplateService.updateDoctorReplyTemplate(accountId, vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除常用回复模板")
    public RestBean<Void> delete(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                 @RequestParam @Positive int id) {
        return this.messageHandle(() -> doctorReplyTemplateService.deleteDoctorReplyTemplate(accountId, id));
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
