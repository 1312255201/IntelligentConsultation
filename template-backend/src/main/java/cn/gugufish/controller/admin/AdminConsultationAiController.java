package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.response.ConsultationAiAuditItemVO;
import cn.gugufish.entity.vo.response.ConsultationAiOverviewVO;
import cn.gugufish.service.ConsultationAiAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/admin/consultation-ai")
@Tag(name = "管理员 AI 导诊配置", description = "查看 Spring AI + DeepSeek 运行状态、当前参数和 AI 导诊产出概况")
public class AdminConsultationAiController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv;charset=UTF-8");
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> AUDIT_MESSAGE_TYPES = Set.of(
            "all",
            "ai_triage_summary",
            "ai_followup_questions",
            "ai_chat_reply"
    );

    @Resource
    ConsultationAiAdminService consultationAiAdminService;

    @GetMapping("/overview")
    @Operation(summary = "查询 AI 导诊运行概览")
    public RestBean<ConsultationAiOverviewVO> overview() {
        return RestBean.success(consultationAiAdminService.overview());
    }

    @GetMapping("/audit-list")
    @Operation(summary = "查询最近 AI 导诊输出审计列表")
    public RestBean<List<ConsultationAiAuditItemVO>> auditList(@RequestParam(defaultValue = "all") String messageType,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "false") boolean highRiskOnly,
                                                               @RequestParam(defaultValue = "20") @Positive int limit) {
        String normalizedMessageType = normalizeMessageType(messageType);
        if (normalizedMessageType == null) {
            return RestBean.failure(400, "Invalid AI message type");
        }
        return RestBean.success(consultationAiAdminService.auditList(
                normalizedMessageType,
                keyword,
                highRiskOnly,
                Math.min(limit, 50)
        ));
    }

    @GetMapping("/audit-list/export")
    @Operation(summary = "导出 AI 导诊输出审计样本 CSV")
    public ResponseEntity<?> exportAuditList(@RequestParam(defaultValue = "all") String messageType,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "false") boolean highRiskOnly,
                                             @RequestParam(defaultValue = "100") @Positive int limit) {
        String normalizedMessageType = normalizeMessageType(messageType);
        if (normalizedMessageType == null) {
            return ResponseEntity.badRequest().body(RestBean.failure(400, "Invalid AI message type"));
        }
        return buildCsvResponse(
                consultationAiAdminService.exportAuditListCsv(
                        normalizedMessageType,
                        keyword,
                        highRiskOnly,
                        Math.min(limit, 200)
                ),
                "consultation-ai-audit"
        );
    }

    @GetMapping("/high-risk-review-queue")
    @Operation(summary = "查询高风险 AI 样本待复核队列")
    public RestBean<List<ConsultationAiAuditItemVO>> highRiskReviewQueue(@RequestParam(required = false) String keyword,
                                                                         @RequestParam(defaultValue = "8") @Positive int limit) {
        return RestBean.success(consultationAiAdminService.highRiskReviewQueue(keyword, Math.min(limit, 50)));
    }

    @GetMapping("/high-risk-review-queue/export")
    @Operation(summary = "导出高风险 AI 样本待复核队列 CSV")
    public ResponseEntity<byte[]> exportHighRiskReviewQueue(@RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "100") @Positive int limit) {
        return buildCsvResponse(
                consultationAiAdminService.exportHighRiskReviewQueueCsv(keyword, Math.min(limit, 200)),
                "consultation-ai-high-risk-review"
        );
    }

    private String normalizeMessageType(String messageType) {
        String normalizedMessageType = messageType == null ? "all" : messageType.trim().toLowerCase();
        return AUDIT_MESSAGE_TYPES.contains(normalizedMessageType) ? normalizedMessageType : null;
    }

    private ResponseEntity<byte[]> buildCsvResponse(byte[] content, String fileNamePrefix) {
        String fileName = fileNamePrefix + "-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(CSV_MEDIA_TYPE)
                .body(content);
    }
}
