package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.TriageCaseReferenceCreateVO;
import cn.gugufish.entity.vo.request.TriageCaseReferenceUpdateVO;
import cn.gugufish.entity.vo.response.TriageCaseReferenceVO;
import cn.gugufish.service.TriageCaseReferenceService;
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
@RequestMapping("/api/admin/triage-case")
@Tag(name = "管理员导诊案例库维护", description = "管理员对供 AI 导诊和运营复盘使用的内部案例进行维护")
public class AdminTriageCaseReferenceController {

    @Resource
    TriageCaseReferenceService triageCaseReferenceService;

    @GetMapping("/list")
    @Operation(summary = "查询导诊案例列表")
    public RestBean<List<TriageCaseReferenceVO>> list() {
        return RestBean.success(triageCaseReferenceService.listCaseReferences()
                .stream()
                .map(item -> item.asViewObject(TriageCaseReferenceVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增导诊案例")
    public RestBean<Void> create(@RequestBody @Valid TriageCaseReferenceCreateVO vo) {
        return this.messageHandle(() -> triageCaseReferenceService.createCaseReference(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新导诊案例")
    public RestBean<Void> update(@RequestBody @Valid TriageCaseReferenceUpdateVO vo) {
        return this.messageHandle(() -> triageCaseReferenceService.updateCaseReference(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除导诊案例")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> triageCaseReferenceService.deleteCaseReference(id));
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
