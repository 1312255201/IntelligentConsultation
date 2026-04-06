package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.vo.request.ConsultationDispatchBatchPreviewRequestVO;
import cn.gugufish.entity.vo.request.ConsultationDispatchConfigSaveVO;
import cn.gugufish.entity.vo.request.ConsultationDispatchPreviewRequestVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchBatchCompareVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchConfigVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchPreviewRecordVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchPreviewVO;
import cn.gugufish.service.ConsultationDispatchAdminService;
import cn.gugufish.service.ConsultationDispatchConfigService;
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

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/admin/consultation-dispatch")
@Tag(name = "管理员智能分配策略", description = "维护 AI 导诊推荐医生排序权重、运营阈值与策略预览")
public class AdminConsultationDispatchController {

    @Resource
    ConsultationDispatchConfigService consultationDispatchConfigService;

    @Resource
    ConsultationDispatchAdminService consultationDispatchAdminService;

    @GetMapping("/config")
    @Operation(summary = "读取智能分配策略")
    public RestBean<ConsultationDispatchConfigVO> config() {
        ConsultationDispatchConfig config = consultationDispatchConfigService.getConfig();
        return RestBean.success(config.asViewObject(ConsultationDispatchConfigVO.class));
    }

    @PostMapping("/config")
    @Operation(summary = "保存智能分配策略")
    public RestBean<Void> saveConfig(@RequestBody @Valid ConsultationDispatchConfigSaveVO vo) {
        return this.messageHandle(() -> consultationDispatchConfigService.saveConfig(vo));
    }

    @GetMapping("/preview-records")
    @Operation(summary = "查询策略预览样本")
    public RestBean<List<ConsultationDispatchPreviewRecordVO>> previewRecords(@RequestParam(defaultValue = "18") @Positive int limit) {
        return RestBean.success(consultationDispatchAdminService.listPreviewRecords(limit));
    }

    @PostMapping("/preview")
    @Operation(summary = "按当前参数预览推荐医生")
    public RestBean<ConsultationDispatchPreviewVO> preview(@RequestBody @Valid ConsultationDispatchPreviewRequestVO vo) {
        String validateMessage = validateScoreRange(vo.getMinRecommendationScore(), vo.getMaxRecommendationScore());
        if (validateMessage != null) {
            return RestBean.failure(400, validateMessage);
        }
        ConsultationDispatchPreviewVO preview = consultationDispatchAdminService.preview(
                vo.getConsultationId(),
                buildPreviewConfig(vo)
        );
        return preview == null ? RestBean.failure(404, "问诊样本不存在") : RestBean.success(preview);
    }

    @PostMapping("/batch-preview")
    @Operation(summary = "批量对比已保存策略与当前参数")
    public RestBean<ConsultationDispatchBatchCompareVO> batchPreview(@RequestBody @Valid ConsultationDispatchBatchPreviewRequestVO vo) {
        String validateMessage = validateScoreRange(vo.getMinRecommendationScore(), vo.getMaxRecommendationScore());
        if (validateMessage != null) {
            return RestBean.failure(400, validateMessage);
        }
        return RestBean.success(consultationDispatchAdminService.batchPreview(
                vo.getConsultationIds(),
                buildPreviewConfig(vo)
        ));
    }

    private ConsultationDispatchConfig buildPreviewConfig(ConsultationDispatchConfigSaveVO vo) {
        return new ConsultationDispatchConfig(
                null,
                vo.getVisitTypeWeight(),
                vo.getScheduleWeight(),
                vo.getCapacityWeight(),
                vo.getWorkloadWeight(),
                vo.getTagMatchWeight(),
                vo.getTagMatchScorePerHit(),
                vo.getMaxMatchedTags(),
                vo.getRecommendDoctorLimit(),
                vo.getMinRecommendationScore(),
                vo.getMaxRecommendationScore(),
                vo.getWaitingOverdueHours(),
                new Date()
        );
    }

    private String validateScoreRange(Integer minScore, Integer maxScore) {
        if (minScore != null && maxScore != null && minScore > maxScore) {
            return "推荐分下限不能大于上限";
        }
        return null;
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
