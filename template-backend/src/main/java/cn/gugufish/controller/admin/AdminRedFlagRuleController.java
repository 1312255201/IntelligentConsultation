package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.RedFlagRuleCreateVO;
import cn.gugufish.entity.vo.request.RedFlagRuleUpdateVO;
import cn.gugufish.entity.vo.response.RedFlagRuleVO;
import cn.gugufish.service.RedFlagRuleService;
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
@RequestMapping("/api/admin/red-flag")
@Tag(name = "管理员红旗规则维护", description = "管理员对智能导诊使用的高风险红旗规则进行维护")
public class AdminRedFlagRuleController {

    @Resource
    RedFlagRuleService redFlagRuleService;

    @GetMapping("/list")
    @Operation(summary = "查询红旗规则列表")
    public RestBean<List<RedFlagRuleVO>> list() {
        return RestBean.success(redFlagRuleService.listRules());
    }

    @PostMapping("/create")
    @Operation(summary = "新增红旗规则")
    public RestBean<Void> create(@RequestBody @Valid RedFlagRuleCreateVO vo) {
        return this.messageHandle(() -> redFlagRuleService.createRule(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新红旗规则")
    public RestBean<Void> update(@RequestBody @Valid RedFlagRuleUpdateVO vo) {
        return this.messageHandle(() -> redFlagRuleService.updateRule(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除红旗规则")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> redFlagRuleService.deleteRule(id));
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
