package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.TriageLevelDictCreateVO;
import cn.gugufish.entity.vo.request.TriageLevelDictUpdateVO;
import cn.gugufish.entity.vo.response.TriageLevelDictVO;
import cn.gugufish.service.TriageLevelDictService;
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
@RequestMapping("/api/admin/triage-level")
@Tag(name = "管理员分诊等级字典维护", description = "管理员对智能导诊使用的分诊等级字典进行维护")
public class AdminTriageLevelDictController {

    @Resource
    TriageLevelDictService triageLevelDictService;

    @GetMapping("/list")
    @Operation(summary = "查询分诊等级字典列表")
    public RestBean<List<TriageLevelDictVO>> list() {
        return RestBean.success(triageLevelDictService.listTriageLevels()
                .stream()
                .map(item -> item.asViewObject(TriageLevelDictVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增分诊等级字典")
    public RestBean<Void> create(@RequestBody @Valid TriageLevelDictCreateVO vo) {
        return this.messageHandle(() -> triageLevelDictService.createTriageLevel(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新分诊等级字典")
    public RestBean<Void> update(@RequestBody @Valid TriageLevelDictUpdateVO vo) {
        return this.messageHandle(() -> triageLevelDictService.updateTriageLevel(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除分诊等级字典")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> triageLevelDictService.deleteTriageLevel(id));
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
