package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.BodyPartDictCreateVO;
import cn.gugufish.entity.vo.request.BodyPartDictUpdateVO;
import cn.gugufish.entity.vo.response.BodyPartDictVO;
import cn.gugufish.service.BodyPartDictService;
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
@RequestMapping("/api/admin/body-part")
@Tag(name = "管理员身体部位字典维护", description = "管理员对导诊用身体部位字典进行维护")
public class AdminBodyPartDictController {

    @Resource
    BodyPartDictService bodyPartDictService;

    @GetMapping("/list")
    @Operation(summary = "查询身体部位字典列表")
    public RestBean<List<BodyPartDictVO>> list() {
        return RestBean.success(bodyPartDictService.listBodyParts()
                .stream()
                .map(item -> item.asViewObject(BodyPartDictVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增身体部位字典")
    public RestBean<Void> create(@RequestBody @Valid BodyPartDictCreateVO vo) {
        return this.messageHandle(() -> bodyPartDictService.createBodyPart(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新身体部位字典")
    public RestBean<Void> update(@RequestBody @Valid BodyPartDictUpdateVO vo) {
        return this.messageHandle(() -> bodyPartDictService.updateBodyPart(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除身体部位字典")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> bodyPartDictService.deleteBodyPart(id));
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
