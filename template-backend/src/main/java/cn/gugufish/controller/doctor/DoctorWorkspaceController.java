package cn.gugufish.controller.doctor;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.request.DoctorConsultationAiDraftGenerateVO;
import cn.gugufish.entity.vo.request.DoctorConsultationAssignSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFollowUpSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationFormDraftApplyVO;
import cn.gugufish.entity.vo.request.DoctorConsultationHandleSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationMessageDraftApplyVO;
import cn.gugufish.entity.vo.request.DoctorConsultationMessageDraftGenerateVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
import cn.gugufish.entity.vo.response.DoctorConsultationFollowUpDraftVO;
import cn.gugufish.entity.vo.response.DoctorConsultationHandleDraftVO;
import cn.gugufish.entity.vo.response.DoctorConsultationMessageDraftVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;
import cn.gugufish.service.ConsultationMessageService;
import cn.gugufish.service.DoctorWorkspaceService;
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

@Validated
@RestController
@RequestMapping("/api/doctor")
@Tag(name = "Doctor Workspace", description = "医生工作台概览、问诊认领处理与排班查询")
public class DoctorWorkspaceController {

    @Resource
    DoctorWorkspaceService doctorWorkspaceService;

    @Resource
    ConsultationMessageService consultationMessageService;

    @GetMapping("/workbench/summary")
    @Operation(summary = "查询医生工作台概览")
    public RestBean<DoctorWorkbenchVO> workbench(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(doctorWorkspaceService.workbench(accountId));
    }

    @GetMapping("/consultation/list")
    @Operation(summary = "查询当前医生可查看的问诊列表")
    public RestBean<List<AdminConsultationRecordVO>> consultationList(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(doctorWorkspaceService.consultationList(accountId));
    }

    @GetMapping("/consultation/detail")
    @Operation(summary = "查询医生侧问诊详情")
    public RestBean<AdminConsultationRecordVO> consultationDetail(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                                  @RequestParam("id") @Positive int recordId) {
        AdminConsultationRecordVO record = doctorWorkspaceService.consultationDetail(accountId, recordId);
        return record == null
                ? RestBean.failure(404, "当前问诊记录不存在或暂无查看权限")
                : RestBean.success(record);
    }

    @GetMapping("/consultation/message/list")
    @Operation(summary = "查询问诊沟通消息")
    public RestBean<List<ConsultationMessageVO>> consultationMessages(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                                      @RequestParam @Positive int recordId) {
        List<ConsultationMessageVO> messages = consultationMessageService.listDoctorMessages(accountId, recordId);
        return messages == null
                ? RestBean.failure(404, "当前问诊记录不存在或暂无查看权限")
                : RestBean.success(messages);
    }

    @PostMapping("/consultation/handle/ai-draft")
    @Operation(summary = "生成医生处理表单 AI 草稿")
    public RestBean<DoctorConsultationHandleDraftVO> generateConsultationHandleDraft(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                                                     @RequestBody @Valid DoctorConsultationAiDraftGenerateVO vo) {
        DoctorConsultationHandleDraftVO draft = doctorWorkspaceService.generateConsultationHandleDraft(accountId, vo);
        return draft == null
                ? RestBean.failure(404, "当前问诊记录不存在或暂无查看权限")
                : RestBean.success(draft);
    }

    @PostMapping("/consultation/follow-up/ai-draft")
    @Operation(summary = "生成医生随访表单 AI 草稿")
    public RestBean<DoctorConsultationFollowUpDraftVO> generateConsultationFollowUpDraft(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                                                         @RequestBody @Valid DoctorConsultationAiDraftGenerateVO vo) {
        DoctorConsultationFollowUpDraftVO draft = doctorWorkspaceService.generateConsultationFollowUpDraft(accountId, vo);
        return draft == null
                ? RestBean.failure(404, "当前问诊记录不存在或暂无查看权限")
                : RestBean.success(draft);
    }

    @PostMapping("/consultation/form/ai-draft/apply")
    @Operation(summary = "记录医生处理/随访 AI 草稿带入行为")
    public RestBean<Void> trackConsultationFormDraftApply(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                          @RequestBody @Valid DoctorConsultationFormDraftApplyVO vo) {
        String message = doctorWorkspaceService.trackConsultationFormDraftApply(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/consultation/message/ai-draft")
    @Operation(summary = "生成医生沟通消息 AI 草稿")
    public RestBean<DoctorConsultationMessageDraftVO> generateConsultationMessageDraft(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                                                       @RequestBody @Valid DoctorConsultationMessageDraftGenerateVO vo) {
        DoctorConsultationMessageDraftVO draft = doctorWorkspaceService.generateConsultationMessageDraft(accountId, vo);
        return draft == null
                ? RestBean.failure(404, "当前问诊记录不存在或暂无查看权限")
                : RestBean.success(draft);
    }

    @PostMapping("/consultation/message/ai-draft/apply")
    @Operation(summary = "记录医生沟通消息 AI 草稿带入行为")
    public RestBean<Void> trackConsultationMessageDraftApply(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                             @RequestBody @Valid DoctorConsultationMessageDraftApplyVO vo) {
        String message = doctorWorkspaceService.trackConsultationMessageDraftApply(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/consultation/message/send")
    @Operation(summary = "发送问诊沟通消息")
    public RestBean<Void> sendConsultationMessage(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                  @RequestBody @Valid ConsultationMessageSendVO vo) {
        String message = consultationMessageService.sendDoctorMessage(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/consultation/claim")
    @Operation(summary = "认领问诊单")
    public RestBean<Void> claimConsultation(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                            @RequestBody @Valid DoctorConsultationAssignSubmitVO vo) {
        String message = doctorWorkspaceService.claimConsultation(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/consultation/release")
    @Operation(summary = "释放问诊单")
    public RestBean<Void> releaseConsultation(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                              @RequestBody @Valid DoctorConsultationAssignSubmitVO vo) {
        String message = doctorWorkspaceService.releaseConsultation(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/consultation/handle")
    @Operation(summary = "提交医生处理结果")
    public RestBean<Void> submitConsultationHandle(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                   @RequestBody @Valid DoctorConsultationHandleSubmitVO vo) {
        String message = doctorWorkspaceService.submitConsultationHandle(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/consultation/follow-up")
    @Operation(summary = "提交医生随访记录")
    public RestBean<Void> submitConsultationFollowUp(@RequestAttribute(Const.ATTR_USER_ID) int accountId,
                                                     @RequestBody @Valid DoctorConsultationFollowUpSubmitVO vo) {
        String message = doctorWorkspaceService.submitConsultationFollowUp(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @GetMapping("/schedule/list")
    @Operation(summary = "查询当前医生排班列表")
    public RestBean<List<DoctorScheduleVO>> scheduleList(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(doctorWorkspaceService.scheduleList(accountId));
    }
}
