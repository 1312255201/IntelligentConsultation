package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ConsultationRecordCreateVO;
import cn.gugufish.entity.vo.request.ConsultationTriageFeedbackSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackOptionsVO;
import cn.gugufish.entity.vo.response.ConsultationEntryCategoryVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.entity.vo.response.ConsultationRecordVO;
import cn.gugufish.service.ConsultationService;
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
@RequestMapping("/api/user/consultation")
@Tag(name = "用户发起问诊", description = "用户选择问诊分类、装配前置模板并提交问诊资料")
public class ConsultationController {

    @Resource
    ConsultationService consultationService;

    @GetMapping("/category/list")
    @Operation(summary = "查询可发起的问诊分类列表")
    public RestBean<List<ConsultationEntryCategoryVO>> listCategories() {
        return RestBean.success(consultationService.listEntryCategories());
    }

    @GetMapping("/template/default")
    @Operation(summary = "查询指定问诊分类的默认模板")
    public RestBean<ConsultationIntakeTemplateVO> template(@RequestParam @Positive int categoryId) {
        ConsultationIntakeTemplateVO template = consultationService.defaultTemplateDetail(categoryId);
        return template == null ? RestBean.failure(404, "当前问诊分类暂无可用模板") : RestBean.success(template);
    }

    @GetMapping("/record/list")
    @Operation(summary = "查询当前用户的问诊记录")
    public RestBean<List<ConsultationRecordVO>> listRecords(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(consultationService.listRecords(id));
    }

    @GetMapping("/record/detail")
    @Operation(summary = "查询问诊记录详情")
    public RestBean<ConsultationRecordVO> detail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                                 @RequestParam @Positive int recordId) {
        ConsultationRecordVO record = consultationService.recordDetail(id, recordId);
        return record == null ? RestBean.failure(404, "问诊记录不存在") : RestBean.success(record);
    }

    @PostMapping("/record/create")
    @Operation(summary = "提交问诊资料")
    public RestBean<Void> create(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                 @RequestBody @Valid ConsultationRecordCreateVO vo) {
        return this.messageHandle(() -> consultationService.createRecord(id, vo));
    }

    @GetMapping("/feedback/options")
    @Operation(summary = "查询导诊反馈选项")
    public RestBean<ConsultationFeedbackOptionsVO> feedbackOptions() {
        return RestBean.success(consultationService.feedbackOptions());
    }

    @PostMapping("/feedback/submit")
    @Operation(summary = "提交导诊反馈")
    public RestBean<Void> submitFeedback(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ConsultationTriageFeedbackSubmitVO vo) {
        return this.messageHandle(() -> consultationService.submitTriageFeedback(id, vo));
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
