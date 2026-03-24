package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.PatientMedicalHistorySaveVO;
import cn.gugufish.entity.vo.response.PatientMedicalHistoryVO;
import cn.gugufish.service.PatientMedicalHistoryService;
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
@RequestMapping("/api/user/medical-history")
@Tag(name = "用户健康档案管理", description = "当前登录用户维护就诊人的健康档案与病史信息")
public class PatientMedicalHistoryController {

    @Resource
    PatientMedicalHistoryService patientMedicalHistoryService;

    @GetMapping("/list")
    @Operation(summary = "查询当前账号的健康档案列表")
    public RestBean<List<PatientMedicalHistoryVO>> list(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(patientMedicalHistoryService.listByAccountId(id).stream()
                .map(item -> item.asViewObject(PatientMedicalHistoryVO.class))
                .toList());
    }

    @GetMapping("/detail")
    @Operation(summary = "查询指定就诊人的健康档案")
    public RestBean<PatientMedicalHistoryVO> detail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                                    @RequestParam @Positive int patientId) {
        var history = patientMedicalHistoryService.getByPatientId(id, patientId);
        return RestBean.success(history == null ? null : history.asViewObject(PatientMedicalHistoryVO.class));
    }

    @PostMapping("/save")
    @Operation(summary = "保存就诊人健康档案")
    public RestBean<Void> save(@RequestAttribute(Const.ATTR_USER_ID) int id,
                               @RequestBody @Valid PatientMedicalHistorySaveVO vo) {
        return this.messageHandle(() -> patientMedicalHistoryService.saveHistory(id, vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除就诊人健康档案")
    public RestBean<Void> delete(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                 @RequestParam @Positive int patientId) {
        return this.messageHandle(() -> patientMedicalHistoryService.deleteHistory(id, patientId));
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
