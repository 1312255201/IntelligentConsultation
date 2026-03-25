package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.SymptomDictCreateVO;
import cn.gugufish.entity.vo.request.SymptomDictUpdateVO;
import cn.gugufish.entity.vo.response.SymptomDictVO;
import cn.gugufish.service.SymptomDictService;
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
@RequestMapping("/api/admin/symptom")
@Tag(name = "管理员症状字典维护", description = "管理员对导诊用症状字典进行维护")
public class AdminSymptomDictController {

    @Resource
    SymptomDictService symptomDictService;

    @GetMapping("/list")
    @Operation(summary = "查询症状字典列表")
    public RestBean<List<SymptomDictVO>> list() {
        return RestBean.success(symptomDictService.listSymptoms()
                .stream()
                .map(item -> item.asViewObject(SymptomDictVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增症状字典")
    public RestBean<Void> create(@RequestBody @Valid SymptomDictCreateVO vo) {
        return this.messageHandle(() -> symptomDictService.createSymptom(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新症状字典")
    public RestBean<Void> update(@RequestBody @Valid SymptomDictUpdateVO vo) {
        return this.messageHandle(() -> symptomDictService.updateSymptom(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除症状字典")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> symptomDictService.deleteSymptom(id));
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
