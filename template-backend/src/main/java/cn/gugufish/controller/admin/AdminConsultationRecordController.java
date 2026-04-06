package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.response.AdminConsultationAiFieldSampleVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiMismatchVO;
import cn.gugufish.entity.vo.response.AdminConsultationAiSummaryVO;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.service.ConsultationRecordAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/admin/consultation-record")
@Tag(name = "管理员导诊记录中心", description = "管理员查看用户问诊记录、问诊答案和规则命中留痕")
public class AdminConsultationRecordController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv;charset=UTF-8");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> FIELD_KEYS = Set.of("condition_level", "disposition", "follow_up");
    private static final Set<String> SAMPLE_STATUSES = Set.of("mismatch", "pending");

    @Resource
    ConsultationRecordAdminService consultationRecordAdminService;

    @GetMapping("/list")
    @Operation(summary = "查询导诊记录列表")
    public RestBean<List<AdminConsultationRecordVO>> list() {
        return RestBean.success(consultationRecordAdminService.listRecords());
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "查询 AI 采纳统计摘要")
    public RestBean<AdminConsultationAiSummaryVO> aiSummary() {
        return RestBean.success(consultationRecordAdminService.aiSummary());
    }

    @GetMapping("/mismatch-samples")
    @Operation(summary = "查询 AI 偏差样本")
    public RestBean<List<AdminConsultationAiMismatchVO>> mismatchSamples(@RequestParam(defaultValue = "8") @Positive int limit,
                                                                         @RequestParam(defaultValue = "0") @PositiveOrZero int offset,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String doctorName,
                                                                         @RequestParam(required = false) String reasonCode,
                                                                         @RequestParam(required = false) String categoryName,
                                                                         @RequestParam(required = false) String departmentName) {
        return RestBean.success(consultationRecordAdminService.mismatchSamples(
                Math.min(limit, 50),
                offset,
                keyword,
                doctorName,
                reasonCode,
                categoryName,
                departmentName
        ));
    }

    @GetMapping("/mismatch-samples/export")
    @Operation(summary = "导出 AI 偏差样本 CSV")
    public ResponseEntity<byte[]> exportMismatchSamples(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String doctorName,
                                                        @RequestParam(required = false) String reasonCode,
                                                        @RequestParam(required = false) String categoryName,
                                                        @RequestParam(required = false) String departmentName) {
        return buildCsvResponse(
                consultationRecordAdminService.exportMismatchSamplesCsv(
                        keyword,
                        doctorName,
                        reasonCode,
                        categoryName,
                        departmentName
                ),
                "consultation-mismatch-samples"
        );
    }

    @GetMapping("/field-samples")
    @Operation(summary = "查询字段样本")
    public RestBean<List<AdminConsultationAiFieldSampleVO>> fieldSamples(@RequestParam String fieldKey,
                                                                         @RequestParam(defaultValue = "mismatch") String status,
                                                                         @RequestParam(defaultValue = "8") @Positive int limit,
                                                                         @RequestParam(defaultValue = "0") @PositiveOrZero int offset,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(required = false) String doctorName,
                                                                         @RequestParam(required = false) String categoryName,
                                                                         @RequestParam(required = false) String departmentName) {
        if (!FIELD_KEYS.contains(fieldKey)) {
            return RestBean.failure(400, "Invalid field key");
        }
        if (!SAMPLE_STATUSES.contains(status)) {
            return RestBean.failure(400, "Invalid sample status");
        }
        return RestBean.success(consultationRecordAdminService.fieldSamples(
                fieldKey,
                status,
                Math.min(limit, 50),
                offset,
                keyword,
                doctorName,
                categoryName,
                departmentName
        ));
    }

    @GetMapping("/field-samples/export")
    @Operation(summary = "导出字段样本 CSV")
    public ResponseEntity<?> exportFieldSamples(@RequestParam String fieldKey,
                                                @RequestParam(defaultValue = "mismatch") String status,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String doctorName,
                                                @RequestParam(required = false) String categoryName,
                                                @RequestParam(required = false) String departmentName) {
        if (!FIELD_KEYS.contains(fieldKey)) {
            return ResponseEntity.badRequest().body(RestBean.failure(400, "Invalid field key"));
        }
        if (!SAMPLE_STATUSES.contains(status)) {
            return ResponseEntity.badRequest().body(RestBean.failure(400, "Invalid sample status"));
        }
        return buildCsvResponse(
                consultationRecordAdminService.exportFieldSamplesCsv(
                        fieldKey,
                        status,
                        keyword,
                        doctorName,
                        categoryName,
                        departmentName
                ),
                "consultation-field-samples"
        );
    }

    @GetMapping("/detail")
    @Operation(summary = "查询导诊记录详情")
    public RestBean<AdminConsultationRecordVO> detail(@RequestParam @Positive int id) {
        AdminConsultationRecordVO record = consultationRecordAdminService.recordDetail(id);
        return record == null ? RestBean.failure(404, "Consultation record not found") : RestBean.success(record);
    }

    private ResponseEntity<byte[]> buildCsvResponse(byte[] content, String fileNamePrefix) {
        String fileName = fileNamePrefix + "-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(CSV_MEDIA_TYPE)
                .body(content);
    }
}
