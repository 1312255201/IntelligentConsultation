package cn.gugufish.utils;

import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.vo.response.ConsultationArchiveSummaryVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorConclusionVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorFollowUpVO;
import cn.gugufish.entity.vo.response.ConsultationDoctorHandleVO;
import cn.gugufish.entity.vo.response.ConsultationMessageSummaryVO;
import cn.gugufish.entity.vo.response.ConsultationServiceFeedbackVO;
import cn.gugufish.entity.vo.response.TriageResultVO;
import com.alibaba.fastjson2.JSON;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class ConsultationArchiveSummaryUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ConsultationArchiveSummaryUtils() {
    }

    public static ConsultationArchiveSummaryVO buildDoctorSummary(ConsultationRecord record,
                                                                  ConsultationMessageSummaryVO messageSummary,
                                                                  ConsultationDoctorAssignmentVO doctorAssignment,
                                                                  ConsultationDoctorHandleVO doctorHandle,
                                                                  ConsultationDoctorConclusionVO doctorConclusion,
                                                                  List<ConsultationDoctorFollowUpVO> doctorFollowUps,
                                                                  TriageResultVO triageResult,
                                                                  ConsultationServiceFeedbackVO serviceFeedback) {
        if (record == null) return null;

        ConsultationDoctorFollowUpVO latestFollowUp = latestFollowUp(doctorFollowUps);
        String doctorName = firstText(
                doctorHandle == null ? null : doctorHandle.getDoctorName(),
                doctorAssignment == null ? null : doctorAssignment.getDoctorName(),
                triageResult == null ? null : triageResult.getDoctorName()
        );
        List<String> riskFlags = parseJsonStringList(triageResult == null ? null : triageResult.getRiskFlagsJson());
        List<String> conclusionTags = parseJsonStringList(doctorConclusion == null ? null : doctorConclusion.getConclusionTagsJson());
        List<String> nextActions = buildDoctorNextActions(
                record,
                doctorAssignment,
                messageSummary,
                doctorHandle,
                doctorConclusion,
                latestFollowUp,
                serviceFeedback
        );

        ConsultationArchiveSummaryVO summary = new ConsultationArchiveSummaryVO();
        summary.setStageText(resolveDoctorStageText(record, doctorHandle, doctorConclusion, latestFollowUp));
        summary.setOverview(buildOverview(record));
        summary.setTriageSummary(buildTriageSummary(record, triageResult, riskFlags));
        summary.setDoctorSummary(buildDoctorHandleSummary(doctorHandle, doctorName));
        summary.setArchiveConclusion(buildArchiveConclusion(doctorHandle, doctorConclusion));
        summary.setFollowUpSummary(buildFollowUpSummary(doctorConclusion, latestFollowUp, doctorFollowUps));
        summary.setServiceSummary(buildDoctorServiceSummary(record, doctorHandle, serviceFeedback));
        summary.setLatestMessageSummary(buildDoctorLatestMessageSummary(record, messageSummary));
        summary.setPatientActionHint(resolveDoctorActionHint(nextActions, record, doctorHandle, doctorConclusion));
        summary.setDoctorName(doctorName);
        summary.setLastUpdateTime(resolveLastUpdateTime(record, messageSummary, doctorHandle, doctorConclusion, latestFollowUp, serviceFeedback, triageResult));
        summary.setMessageCount(messageSummary == null ? 0 : defaultInt(messageSummary.getTotalCount()));
        summary.setFollowUpCount(doctorFollowUps == null ? 0 : doctorFollowUps.size());
        summary.setServiceScore(serviceFeedback == null ? null : serviceFeedback.getServiceScore());
        summary.setRiskFlags(riskFlags);
        summary.setConclusionTags(conclusionTags);
        summary.setNextActions(nextActions);
        summary.setPlainText(buildDoctorPlainText(record, summary));
        return summary;
    }

    private static String resolveDoctorStageText(ConsultationRecord record,
                                                 ConsultationDoctorHandleVO doctorHandle,
                                                 ConsultationDoctorConclusionVO doctorConclusion,
                                                 ConsultationDoctorFollowUpVO latestFollowUp) {
        if (latestFollowUp != null && Objects.equals(latestFollowUp.getNeedRevisit(), 0)) {
            return "本次问诊与随访已闭环完成";
        }
        if (doctorConclusion != null) {
            return Objects.equals(doctorConclusion.getNeedFollowUp(), 1)
                    ? "已完成结论，待继续随访"
                    : "已形成本轮归档结论";
        }
        if (doctorHandle != null) {
            return "completed".equalsIgnoreCase(trimToNull(doctorHandle.getStatus()))
                    ? "已完成本轮处理，待后续反馈"
                    : "医生正在处理中";
        }
        if ("triaged".equalsIgnoreCase(trimToNull(record.getStatus()))
                || "processing".equalsIgnoreCase(trimToNull(record.getStatus()))
                || trimToNull(record.getTriageLevelName()) != null) {
            return "已完成分诊，待医生接诊";
        }
        return "待系统分诊";
    }

    private static String buildOverview(ConsultationRecord record) {
        List<String> segments = new ArrayList<>();
        String title = firstText(record.getTitle(), record.getCategoryName());
        if (title != null) segments.add("问诊主题：" + title);
        if (trimToNull(record.getPatientName()) != null) segments.add("患者：" + record.getPatientName());
        if (trimToNull(record.getChiefComplaint()) != null) {
            segments.add("主诉：" + abbreviate(record.getChiefComplaint(), 140));
        }
        if (trimToNull(record.getHealthSummary()) != null) {
            segments.add("健康摘要：" + abbreviate(record.getHealthSummary(), 140));
        }
        return segments.isEmpty()
                ? "患者已提交本次问诊资料，待进一步处理。"
                : String.join("；", segments);
    }

    private static String buildTriageSummary(ConsultationRecord record,
                                             TriageResultVO triageResult,
                                             List<String> riskFlags) {
        List<String> segments = new ArrayList<>();
        if (trimToNull(record.getTriageLevelName()) != null) {
            segments.add("风险分级：" + record.getTriageLevelName());
        }
        if (trimToNull(record.getTriageActionType()) != null) {
            segments.add("建议动作：" + triageActionText(record.getTriageActionType()));
        }
        String departmentName = firstText(
                triageResult == null ? null : triageResult.getDepartmentName(),
                record.getDepartmentName()
        );
        if (departmentName != null) segments.add("建议科室：" + departmentName);
        String triageReason = firstText(
                triageResult == null ? null : triageResult.getReasonText(),
                record.getTriageSuggestion()
        );
        if (triageReason != null) segments.add("分诊建议：" + abbreviate(triageReason, 160));
        if (!riskFlags.isEmpty()) {
            segments.add("重点风险：" + abbreviate(String.join("、", riskFlags), 100));
        }
        return segments.isEmpty()
                ? "系统尚未生成完整的分诊结论。"
                : String.join("；", segments);
    }

    private static String buildDoctorHandleSummary(ConsultationDoctorHandleVO doctorHandle,
                                                   String doctorName) {
        if (doctorHandle == null) {
            return doctorName == null
                    ? "当前尚未形成正式处理记录，可先认领问诊并开始沟通。"
                    : "当前由 " + doctorName + " 跟进，尚未形成正式处理记录。";
        }
        List<String> segments = new ArrayList<>();
        if (doctorName != null) segments.add("跟进医生：" + doctorName);
        segments.add("处理状态：" + doctorHandleStatusText(doctorHandle.getStatus()));
        if (trimToNull(doctorHandle.getSummary()) != null) {
            segments.add("判断摘要：" + abbreviate(doctorHandle.getSummary(), 160));
        }
        if (trimToNull(doctorHandle.getMedicalAdvice()) != null) {
            segments.add("处理建议：" + abbreviate(doctorHandle.getMedicalAdvice(), 160));
        }
        if (trimToNull(doctorHandle.getFollowUpPlan()) != null) {
            segments.add("随访计划：" + abbreviate(doctorHandle.getFollowUpPlan(), 120));
        }
        return segments.isEmpty()
                ? "医生已接诊，正在整理处理意见。"
                : String.join("；", segments);
    }

    private static String buildArchiveConclusion(ConsultationDoctorHandleVO doctorHandle,
                                                 ConsultationDoctorConclusionVO doctorConclusion) {
        if (doctorConclusion == null) {
            if (doctorHandle != null && trimToNull(doctorHandle.getSummary()) != null) {
                return "已完成初步处理，结构化结论仍待补充。";
            }
            return "当前尚未形成最终归档结论。";
        }
        List<String> segments = new ArrayList<>();
        if (trimToNull(doctorConclusion.getConditionLevel()) != null) {
            segments.add("病情等级：" + conditionLevelText(doctorConclusion.getConditionLevel()));
        }
        if (trimToNull(doctorConclusion.getDisposition()) != null) {
            segments.add("处理去向：" + dispositionText(doctorConclusion.getDisposition()));
        }
        if (trimToNull(doctorConclusion.getDiagnosisDirection()) != null) {
            segments.add("诊断方向：" + abbreviate(doctorConclusion.getDiagnosisDirection(), 160));
        }
        if (trimToNull(doctorConclusion.getPatientInstruction()) != null) {
            segments.add("患者指引：" + abbreviate(doctorConclusion.getPatientInstruction(), 160));
        }
        String followUpText = buildConclusionFollowUpText(doctorConclusion);
        if (followUpText != null) segments.add("随访要求：" + followUpText);
        return segments.isEmpty()
                ? "当前尚未形成最终归档结论。"
                : String.join("；", segments);
    }

    private static String buildFollowUpSummary(ConsultationDoctorConclusionVO doctorConclusion,
                                               ConsultationDoctorFollowUpVO latestFollowUp,
                                               List<ConsultationDoctorFollowUpVO> doctorFollowUps) {
        if (latestFollowUp == null) {
            if (doctorConclusion != null && Objects.equals(doctorConclusion.getNeedFollowUp(), 1)) {
                String followUpText = buildConclusionFollowUpText(doctorConclusion);
                return followUpText == null
                        ? "医生已标记需要继续随访，当前尚未记录新的随访结果。"
                        : "医生已要求 " + followUpText + "，当前尚未记录新的随访结果。";
            }
            return "当前暂无随访记录。";
        }
        List<String> segments = new ArrayList<>();
        int followUpCount = doctorFollowUps == null ? 0 : doctorFollowUps.size();
        if (followUpCount > 0) segments.add("累计随访：" + followUpCount + " 次");
        if (trimToNull(latestFollowUp.getFollowUpType()) != null) {
            segments.add("最近方式：" + followUpTypeText(latestFollowUp.getFollowUpType()));
        }
        if (trimToNull(latestFollowUp.getPatientStatus()) != null) {
            segments.add("当前状态：" + patientStatusText(latestFollowUp.getPatientStatus()));
        }
        if (latestFollowUp.getCreateTime() != null) {
            segments.add("最近随访时间：" + formatDateTime(latestFollowUp.getCreateTime()));
        }
        if (trimToNull(latestFollowUp.getSummary()) != null) {
            segments.add("随访摘要：" + abbreviate(latestFollowUp.getSummary(), 160));
        }
        if (trimToNull(latestFollowUp.getAdvice()) != null) {
            segments.add("随访建议：" + abbreviate(latestFollowUp.getAdvice(), 160));
        }
        if (trimToNull(latestFollowUp.getNextStep()) != null) {
            segments.add("下一步安排：" + abbreviate(latestFollowUp.getNextStep(), 120));
        }
        if (latestFollowUp.getNextFollowUpDate() != null) {
            segments.add("下次建议时间：" + formatDateOnly(latestFollowUp.getNextFollowUpDate()));
        }
        if (Objects.equals(latestFollowUp.getNeedRevisit(), 0)) {
            segments.add("本轮随访已完成");
        }
        return segments.isEmpty()
                ? "当前暂无随访记录。"
                : String.join("；", segments);
    }

    private static String buildDoctorServiceSummary(ConsultationRecord record,
                                                    ConsultationDoctorHandleVO doctorHandle,
                                                    ConsultationServiceFeedbackVO serviceFeedback) {
        if (serviceFeedback == null) {
            boolean completed = "completed".equalsIgnoreCase(trimToNull(record.getStatus()))
                    || (doctorHandle != null && "completed".equalsIgnoreCase(trimToNull(doctorHandle.getStatus())));
            return completed
                    ? "本轮问诊已完成，患者尚未提交服务评价。"
                    : "当前尚未收到服务评价。";
        }
        List<String> segments = new ArrayList<>();
        if (serviceFeedback.getServiceScore() != null) {
            segments.add("服务评分：" + serviceFeedback.getServiceScore() + "/5");
        }
        segments.add("问题状态：" + serviceResolvedText(serviceFeedback.getIsResolved()));
        segments.add("评价处理：" + serviceFeedbackHandleStatusText(serviceFeedback.getDoctorHandleStatus()));
        if (trimToNull(serviceFeedback.getFeedbackText()) != null) {
            segments.add("患者评价：" + abbreviate(serviceFeedback.getFeedbackText(), 160));
        }
        if (trimToNull(serviceFeedback.getDoctorHandleRemark()) != null) {
            segments.add("处理备注：" + abbreviate(serviceFeedback.getDoctorHandleRemark(), 160));
        }
        return String.join("；", segments);
    }

    private static String buildDoctorLatestMessageSummary(ConsultationRecord record,
                                                          ConsultationMessageSummaryVO messageSummary) {
        if (messageSummary == null || defaultInt(messageSummary.getTotalCount()) <= 0) {
            return "当前还没有医患沟通消息。";
        }
        List<String> segments = new ArrayList<>();
        segments.add("累计沟通：" + defaultInt(messageSummary.getTotalCount()) + " 条");
        if (messageSummary.getLatestTime() != null) {
            segments.add("最近更新：" + formatDateTime(messageSummary.getLatestTime()));
        }
        String latestSender = latestMessageSenderText(record, messageSummary);
        if (latestSender != null) segments.add("最近发送方：" + latestSender);
        if (trimToNull(messageSummary.getLatestMessagePreview()) != null) {
            segments.add("最近内容：" + abbreviate(messageSummary.getLatestMessagePreview(), 160));
        }
        if (defaultInt(messageSummary.getUnreadCount()) > 0) {
            boolean followUpUpdate = "followup_update".equalsIgnoreCase(trimToNull(messageSummary.getLatestMessageType()));
            segments.add((followUpUpdate ? "待处理恢复更新：" : "待处理患者消息：") + messageSummary.getUnreadCount() + " 条");
        }
        return String.join("；", segments);
    }

    private static List<String> buildDoctorNextActions(ConsultationRecord record,
                                                       ConsultationDoctorAssignmentVO doctorAssignment,
                                                       ConsultationMessageSummaryVO messageSummary,
                                                       ConsultationDoctorHandleVO doctorHandle,
                                                       ConsultationDoctorConclusionVO doctorConclusion,
                                                       ConsultationDoctorFollowUpVO latestFollowUp,
                                                       ConsultationServiceFeedbackVO serviceFeedback) {
        List<String> actions = new ArrayList<>();

        boolean claimed = doctorAssignment != null && "claimed".equalsIgnoreCase(trimToNull(doctorAssignment.getStatus()));
        if (!claimed && !"completed".equalsIgnoreCase(trimToNull(record.getStatus()))) {
            addAction(actions, "先认领当前问诊单，再继续沟通和处理，避免多人重复接单。");
        }

        if (messageSummary != null && defaultInt(messageSummary.getUnreadCount()) > 0) {
            boolean followUpUpdate = "followup_update".equalsIgnoreCase(trimToNull(messageSummary.getLatestMessageType()));
            addAction(actions, followUpUpdate
                    ? "查看患者最新恢复更新，并判断是否需要补充随访或调整建议。"
                    : "查看患者最新补充信息，并及时回复或更新处理意见。");
        }

        if (doctorHandle == null) {
            addAction(actions, "补充本轮处理摘要、处理建议和随访计划，形成首轮处置记录。");
        }
        if (doctorHandle != null && doctorConclusion == null) {
            addAction(actions, "完善结构化结论，明确病情等级、处理去向和随访要求。");
        }

        String disposition = trimToNull(doctorConclusion == null ? null : doctorConclusion.getDisposition());
        if ("emergency".equalsIgnoreCase(disposition)) {
            addAction(actions, "重点确认是否已完成急诊提醒，必要时再次通过消息强调风险。");
        } else if ("offline_visit".equalsIgnoreCase(disposition)) {
            addAction(actions, "重点确认患者是否已安排线下就医或检查，并补充回访计划。");
        }

        if (doctorConclusion != null && Objects.equals(doctorConclusion.getNeedFollowUp(), 1)) {
            if (latestFollowUp != null && Objects.equals(latestFollowUp.getNeedRevisit(), 0)) {
                addAction(actions, "本轮随访已结束，如患者后续再次不适可重新发起问诊。");
            } else if (latestFollowUp != null && latestFollowUp.getNextFollowUpDate() != null) {
                addAction(actions, "请在 " + formatDateOnly(latestFollowUp.getNextFollowUpDate()) + " 前后完成下一次随访记录。");
            } else if (doctorConclusion.getFollowUpWithinDays() != null) {
                addAction(actions, "建议在 " + doctorConclusion.getFollowUpWithinDays() + " 天内完成下一次随访，并记录恢复变化。");
            } else {
                addAction(actions, "当前仍需继续随访，请补充恢复评估与下一步安排。");
            }
        }

        if (isAttentionServiceFeedback(serviceFeedback)) {
            addAction(actions, "患者已提交需关注的服务评价，建议尽快登记回看与处理结果。");
        } else if ("completed".equalsIgnoreCase(trimToNull(record.getStatus())) && serviceFeedback == null) {
            addAction(actions, "本轮处理已完成，可关注患者后续恢复反馈或服务评价。");
        }

        if (actions.isEmpty()) {
            addAction(actions, "当前归档信息已较完整，可持续关注后续消息与随访进展。");
        }
        return actions.stream().limit(5).toList();
    }

    private static String resolveDoctorActionHint(List<String> nextActions,
                                                  ConsultationRecord record,
                                                  ConsultationDoctorHandleVO doctorHandle,
                                                  ConsultationDoctorConclusionVO doctorConclusion) {
        if (nextActions != null && !nextActions.isEmpty()) return nextActions.get(0);
        if (doctorConclusion != null && Objects.equals(doctorConclusion.getNeedFollowUp(), 1)) {
            return "当前仍处于随访观察期，请按计划持续跟进。";
        }
        if (doctorHandle == null) return "当前尚未形成正式处理记录，可先补充首轮处理意见。";
        if ("triaged".equalsIgnoreCase(trimToNull(record.getStatus()))) return "当前已完成分诊，可结合病情尽快接诊处理。";
        return "当前暂无额外处理提醒。";
    }

    private static Date resolveLastUpdateTime(ConsultationRecord record,
                                              ConsultationMessageSummaryVO messageSummary,
                                              ConsultationDoctorHandleVO doctorHandle,
                                              ConsultationDoctorConclusionVO doctorConclusion,
                                              ConsultationDoctorFollowUpVO latestFollowUp,
                                              ConsultationServiceFeedbackVO serviceFeedback,
                                              TriageResultVO triageResult) {
        Date latest = maxDate(record.getUpdateTime(), record.getCreateTime());
        latest = maxDate(latest, messageSummary == null ? null : messageSummary.getLatestTime());
        latest = maxDate(latest, doctorHandle == null ? null : maxDate(doctorHandle.getUpdateTime(), doctorHandle.getCompleteTime()));
        latest = maxDate(latest, doctorConclusion == null ? null : doctorConclusion.getUpdateTime());
        latest = maxDate(latest, latestFollowUp == null ? null : maxDate(latestFollowUp.getUpdateTime(), latestFollowUp.getCreateTime()));
        latest = maxDate(latest, serviceFeedback == null ? null : maxDate(serviceFeedback.getUpdateTime(), serviceFeedback.getCreateTime()));
        latest = maxDate(latest, triageResult == null ? null : maxDate(triageResult.getUpdateTime(), triageResult.getCreateTime()));
        return latest;
    }

    private static String buildDoctorPlainText(ConsultationRecord record,
                                               ConsultationArchiveSummaryVO summary) {
        StringBuilder builder = new StringBuilder();
        appendPlainText(builder, "问诊编号", record.getConsultationNo());
        appendPlainText(builder, "当前阶段", summary.getStageText());
        appendPlainText(builder, "跟进医生", summary.getDoctorName());
        appendPlainText(builder, "最近更新", formatDateTime(summary.getLastUpdateTime()));
        appendPlainText(builder, "问诊概览", summary.getOverview());
        appendPlainText(builder, "分诊结论", summary.getTriageSummary());
        appendPlainText(builder, "医生处理", summary.getDoctorSummary());
        appendPlainText(builder, "归档结论", summary.getArchiveConclusion());
        appendPlainText(builder, "随访进展", summary.getFollowUpSummary());
        appendPlainText(builder, "服务评价", summary.getServiceSummary());
        appendPlainText(builder, "最近沟通", summary.getLatestMessageSummary());
        appendPlainText(builder, "当前处理提醒", summary.getPatientActionHint());
        if (summary.getRiskFlags() != null && !summary.getRiskFlags().isEmpty()) {
            appendPlainText(builder, "风险提示", String.join("、", summary.getRiskFlags()));
        }
        if (summary.getConclusionTags() != null && !summary.getConclusionTags().isEmpty()) {
            appendPlainText(builder, "结论标签", String.join("、", summary.getConclusionTags()));
        }
        if (summary.getNextActions() != null && !summary.getNextActions().isEmpty()) {
            builder.append("后续动作：").append('\n');
            for (int i = 0; i < summary.getNextActions().size(); i++) {
                builder.append(i + 1).append(". ").append(summary.getNextActions().get(i)).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private static void appendPlainText(StringBuilder builder,
                                        String label,
                                        String value) {
        String text = trimToNull(value);
        if (text == null) return;
        if (builder.length() > 0) builder.append('\n');
        builder.append(label).append("：").append(text);
    }

    private static void addAction(List<String> actions,
                                  String action) {
        String text = trimToNull(action);
        if (text == null || actions.contains(text)) return;
        actions.add(text);
    }

    private static ConsultationDoctorFollowUpVO latestFollowUp(List<ConsultationDoctorFollowUpVO> doctorFollowUps) {
        return doctorFollowUps == null || doctorFollowUps.isEmpty() ? null : doctorFollowUps.get(0);
    }

    private static List<String> parseJsonStringList(String json) {
        String text = trimToNull(json);
        if (text == null) return List.of();
        try {
            List<String> values = JSON.parseArray(text, String.class);
            if (values == null) return List.of();
            return values.stream()
                    .map(ConsultationArchiveSummaryUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String latestMessageSenderText(ConsultationRecord record,
                                                  ConsultationMessageSummaryVO messageSummary) {
        String senderType = trimToNull(messageSummary == null ? null : messageSummary.getLatestSenderType());
        if ("user".equalsIgnoreCase(senderType)) {
            return firstText(record == null ? null : record.getPatientName(), messageSummary.getLatestSenderName(), "患者");
        }
        if ("doctor".equalsIgnoreCase(senderType)) {
            return firstText(messageSummary.getLatestSenderName(), "医生");
        }
        return firstText(messageSummary == null ? null : messageSummary.getLatestSenderName(), "系统");
    }

    private static String buildConclusionFollowUpText(ConsultationDoctorConclusionVO doctorConclusion) {
        if (doctorConclusion == null || !Objects.equals(doctorConclusion.getNeedFollowUp(), 1)) return null;
        if (doctorConclusion.getFollowUpWithinDays() != null && doctorConclusion.getFollowUpWithinDays() > 0) {
            return "建议 " + doctorConclusion.getFollowUpWithinDays() + " 天内继续随访";
        }
        return "建议继续随访观察";
    }

    private static String triageActionText(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value).toLowerCase()) {
            case "emergency" -> "立即急诊";
            case "offline" -> "尽快线下就医";
            case "followup" -> "继续线上随访";
            case "online" -> "继续线上问诊";
            case "observe" -> "居家观察";
            default -> "继续关注";
        };
    }

    private static String doctorHandleStatusText(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value).toLowerCase()) {
            case "completed" -> "已完成";
            case "processing", "in_progress" -> "处理中";
            default -> "待处理";
        };
    }

    private static String conditionLevelText(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value).toLowerCase()) {
            case "low" -> "轻度";
            case "medium" -> "中度";
            case "high" -> "较高风险";
            case "critical" -> "危急";
            default -> "未填写";
        };
    }

    private static String dispositionText(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value).toLowerCase()) {
            case "observe" -> "居家观察";
            case "online_followup" -> "继续线上随访";
            case "offline_visit" -> "建议线下就医";
            case "emergency" -> "建议立即急诊";
            default -> "未填写";
        };
    }

    private static String followUpTypeText(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value).toLowerCase()) {
            case "platform" -> "平台随访";
            case "phone" -> "电话随访";
            case "offline" -> "线下随访";
            case "other" -> "其他方式";
            default -> "其他方式";
        };
    }

    private static String patientStatusText(String value) {
        return switch (trimToNull(value) == null ? "" : trimToNull(value).toLowerCase()) {
            case "improved" -> "明显好转";
            case "stable" -> "基本稳定";
            case "worsened" -> "出现加重";
            case "other" -> "其他情况";
            default -> "其他情况";
        };
    }

    private static String serviceResolvedText(Integer value) {
        return Objects.equals(value, 1) ? "已解决" : "仍待跟进";
    }

    private static String serviceFeedbackHandleStatusText(Integer value) {
        return Objects.equals(value, 1) ? "已记录处理" : "待回看处理";
    }

    private static boolean isAttentionServiceFeedback(ConsultationServiceFeedbackVO serviceFeedback) {
        if (serviceFeedback == null) return false;
        if (Objects.equals(serviceFeedback.getDoctorHandleStatus(), 1)) return false;
        if (!Objects.equals(serviceFeedback.getIsResolved(), 1)) return true;
        return serviceFeedback.getServiceScore() != null && serviceFeedback.getServiceScore() <= 2;
    }

    private static Date maxDate(Date left,
                                Date right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.after(right) ? left : right;
    }

    private static String formatDateTime(Date value) {
        if (value == null) return null;
        return value.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_TIME_FORMATTER);
    }

    private static String formatDateOnly(Date value) {
        if (value == null) return null;
        return value.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_FORMATTER);
    }

    private static String abbreviate(String value,
                                     int maxLength) {
        String text = trimToNull(value);
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, Math.max(maxLength - 3, 0)) + "...";
    }

    private static String firstText(String... values) {
        if (values == null) return null;
        for (String value : values) {
            String text = trimToNull(value);
            if (text != null) return text;
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
