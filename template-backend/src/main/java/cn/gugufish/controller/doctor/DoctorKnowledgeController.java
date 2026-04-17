package cn.gugufish.controller.doctor;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.TriageKnowledgeCreateVO;
import cn.gugufish.entity.vo.request.TriageKnowledgeUpdateVO;
import cn.gugufish.entity.vo.response.TriageKnowledgeVO;
import cn.gugufish.service.DoctorKnowledgeService;
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
@RequestMapping("/api/doctor/knowledge")
@Tag(name = "Doctor Knowledge Management", description = "医生维护个人医学知识沉淀")
public class DoctorKnowledgeController {

    @Resource
    DoctorKnowledgeService doctorKnowledgeService;

    @GetMapping("/list")
    @Operation(summary = "查询医生个人知识库")
    public RestBean<List<TriageKnowledgeVO>> list(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(doctorKnowledgeService.listKnowledgeItems(accountId));
    }

    @PostMapping("/create")
    @Operation(summary = "新增医生个人知识")
    public RestBean<Void> create(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                 @RequestBody @Valid TriageKnowledgeCreateVO vo) {
        return this.messageHandle(() -> doctorKnowledgeService.createKnowledge(accountId, vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新医生个人知识")
    public RestBean<Void> update(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                 @RequestBody @Valid TriageKnowledgeUpdateVO vo) {
        return this.messageHandle(() -> doctorKnowledgeService.updateKnowledge(accountId, vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除医生个人知识")
    public RestBean<Void> delete(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                 @RequestParam @Positive int id) {
        return this.messageHandle(() -> doctorKnowledgeService.deleteKnowledge(accountId, id));
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
