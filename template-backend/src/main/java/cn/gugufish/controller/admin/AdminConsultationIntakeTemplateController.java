package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ConsultationIntakeTemplateCreateVO;
import cn.gugufish.entity.vo.request.ConsultationIntakeTemplateUpdateVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.service.ConsultationIntakeTemplateService;
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
@RequestMapping("/api/admin/consultation-template")
@Tag(name = "管理员问诊前置模板维护", description = "管理员对问诊前置采集模板和字段进行维护")
public class AdminConsultationIntakeTemplateController {

    @Resource
    ConsultationIntakeTemplateService consultationIntakeTemplateService;

    @GetMapping("/list")
    @Operation(summary = "查询问诊前置模板列表")
    public RestBean<List<ConsultationIntakeTemplateVO>> list() {
        return RestBean.success(consultationIntakeTemplateService.listTemplates());
    }

    @GetMapping("/detail")
    @Operation(summary = "查询问诊前置模板详情")
    public RestBean<ConsultationIntakeTemplateVO> detail(@RequestParam @Positive int id) {
        ConsultationIntakeTemplateVO vo = consultationIntakeTemplateService.templateDetail(id);
        return vo == null ? RestBean.failure(404, "问诊前置模板不存在") : RestBean.success(vo);
    }

    @PostMapping("/create")
    @Operation(summary = "新增问诊前置模板")
    public RestBean<Void> create(@RequestBody @Valid ConsultationIntakeTemplateCreateVO vo) {
        return this.messageHandle(() -> consultationIntakeTemplateService.createTemplate(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新问诊前置模板")
    public RestBean<Void> update(@RequestBody @Valid ConsultationIntakeTemplateUpdateVO vo) {
        return this.messageHandle(() -> consultationIntakeTemplateService.updateTemplate(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除问诊前置模板")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> consultationIntakeTemplateService.deleteTemplate(id));
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
