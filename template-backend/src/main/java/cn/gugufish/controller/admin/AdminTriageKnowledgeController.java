package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.TriageKnowledgeCreateVO;
import cn.gugufish.entity.vo.request.TriageKnowledgeUpdateVO;
import cn.gugufish.entity.vo.response.TriageKnowledgeVO;
import cn.gugufish.service.TriageKnowledgeService;
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
@RequestMapping("/api/admin/triage-knowledge")
@Tag(name = "管理员导诊知识库维护", description = "管理员对导诊知识、分诊说明和案例沉淀进行维护")
public class AdminTriageKnowledgeController {

    @Resource
    TriageKnowledgeService triageKnowledgeService;

    @GetMapping("/list")
    @Operation(summary = "查询导诊知识列表")
    public RestBean<List<TriageKnowledgeVO>> list() {
        return RestBean.success(triageKnowledgeService.listKnowledgeItems()
                .stream()
                .map(item -> item.asViewObject(TriageKnowledgeVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增导诊知识")
    public RestBean<Void> create(@RequestBody @Valid TriageKnowledgeCreateVO vo) {
        return this.messageHandle(() -> triageKnowledgeService.createKnowledge(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新导诊知识")
    public RestBean<Void> update(@RequestBody @Valid TriageKnowledgeUpdateVO vo) {
        return this.messageHandle(() -> triageKnowledgeService.updateKnowledge(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除导诊知识")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> triageKnowledgeService.deleteKnowledge(id));
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
