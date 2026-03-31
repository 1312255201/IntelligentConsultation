package cn.gugufish.controller.doctor;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.DoctorConsultationAssignSubmitVO;
import cn.gugufish.entity.vo.request.DoctorConsultationHandleSubmitVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.DoctorScheduleVO;
import cn.gugufish.entity.vo.response.DoctorWorkbenchVO;
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

    @GetMapping("/schedule/list")
    @Operation(summary = "查询当前医生排班列表")
    public RestBean<List<DoctorScheduleVO>> scheduleList(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(doctorWorkspaceService.scheduleList(accountId));
    }
}
