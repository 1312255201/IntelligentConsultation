package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.request.ConsultationPaymentMockPayVO;
import cn.gugufish.entity.vo.request.ConsultationRecordCreateVO;
import cn.gugufish.entity.vo.request.ConsultationServiceFeedbackSubmitVO;
import cn.gugufish.entity.vo.request.ConsultationTriageMessageSendVO;
import cn.gugufish.entity.vo.request.ConsultationTriageFeedbackSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationFeedbackOptionsVO;
import cn.gugufish.entity.vo.response.ConsultationEntryCategoryVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeTemplateVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
import cn.gugufish.entity.vo.response.ConsultationPaymentVO;
import cn.gugufish.entity.vo.response.ConsultationRecordVO;
import cn.gugufish.service.ConsultationMessageService;
import cn.gugufish.service.ConsultationPaymentService;
import cn.gugufish.service.ConsultationService;
import cn.gugufish.service.ConsultationTriageChatService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/user/consultation")
@Tag(name = "用户发起问诊", description = "用户选择问诊分类、装配前置模板并提交问诊资料")
public class ConsultationController {

    private static final MediaType TEXT_MEDIA_TYPE = MediaType.parseMediaType("text/plain;charset=UTF-8");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Resource
    ConsultationService consultationService;

    @Resource
    ConsultationMessageService consultationMessageService;

    @Resource
    ConsultationPaymentService consultationPaymentService;

    @Resource
    ConsultationTriageChatService consultationTriageChatService;

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

    @GetMapping("/record/archive/export")
    @Operation(summary = "下载问诊归档摘要")
    public ResponseEntity<?> exportArchive(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                           @RequestParam @Positive int recordId) {
        ConsultationRecordVO record = consultationService.recordDetail(id, recordId);
        if (record == null) {
            return ResponseEntity.status(404).body(RestBean.failure(404, "问诊记录不存在"));
        }
        String content = record.getArchiveSummary() == null ? null : record.getArchiveSummary().getPlainText();
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(RestBean.failure(400, "当前问诊暂未生成可导出的归档摘要"));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archiveFileName(record) + "\"")
                .contentType(TEXT_MEDIA_TYPE)
                .body(utf8TextWithBom(content));
    }

    @GetMapping("/message/list")
    @Operation(summary = "查询问诊沟通消息")
    public RestBean<List<ConsultationMessageVO>> listMessages(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                                              @RequestParam @Positive int recordId) {
        List<ConsultationMessageVO> messages = consultationMessageService.listUserMessages(id, recordId);
        return messages == null ? RestBean.failure(404, "问诊记录不存在或暂无查看权限") : RestBean.success(messages);
    }

    @PostMapping("/message/send")
    @Operation(summary = "发送问诊沟通消息")
    public RestBean<Void> sendMessage(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ConsultationMessageSendVO vo) {
        return this.messageHandle(() -> consultationMessageService.sendUserMessage(id, vo));
    }

    @PostMapping("/triage/message/send")
    @Operation(summary = "继续与 AI 导诊交互")
    public RestBean<Void> sendTriageMessage(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                            @RequestBody @Valid ConsultationTriageMessageSendVO vo) {
        return this.messageHandle(() -> consultationTriageChatService.sendUserMessage(id, vo));
    }

    @PostMapping("/record/create")
    @Operation(summary = "提交问诊资料")
    public RestBean<Void> create(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                 @RequestBody @Valid ConsultationRecordCreateVO vo) {
        return this.messageHandle(() -> consultationService.createRecord(id, vo));
    }

    @PostMapping("/payment/mock-pay")
    @Operation(summary = "妯℃嫙鏀粯闂瘖璐圭敤")
    public RestBean<ConsultationPaymentVO> mockPay(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                                   @RequestBody @Valid ConsultationPaymentMockPayVO vo) {
        String message = consultationService.mockPay(id, vo.getRecordId());
        if (message != null) {
            return RestBean.failure(400, message);
        }
        ConsultationPaymentVO payment = consultationPaymentService.detailByConsultationId(vo.getRecordId());
        return payment == null ? RestBean.failure(404, "鏀惰垂璁板綍涓嶅瓨鍦?") : RestBean.success(payment);
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

    @PostMapping("/service-feedback/submit")
    @Operation(summary = "提交问诊服务评价")
    public RestBean<Void> submitServiceFeedback(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                                @RequestBody @Valid ConsultationServiceFeedbackSubmitVO vo) {
        return this.messageHandle(() -> consultationService.submitServiceFeedback(id, vo));
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        if (message == null) {
            return RestBean.success();
        } else {
            return RestBean.failure(400, message);
        }
    }

    private String archiveFileName(ConsultationRecordVO record) {
        String consultationNo = record == null ? null : record.getConsultationNo();
        String suffix = consultationNo == null || consultationNo.isBlank()
                ? LocalDateTime.now().format(EXPORT_TIME_FORMATTER)
                : consultationNo.replaceAll("[^A-Za-z0-9_-]", "-");
        return "consultation-archive-" + suffix + ".txt";
    }

    private byte[] utf8TextWithBom(String content) {
        byte[] textBytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[textBytes.length + 3];
        result[0] = (byte) 0xEF;
        result[1] = (byte) 0xBB;
        result[2] = (byte) 0xBF;
        System.arraycopy(textBytes, 0, result, 3, textBytes.length);
        return result;
    }
}
