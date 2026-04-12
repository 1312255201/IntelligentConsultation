package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationDoctorConclusion;
import cn.gugufish.entity.dto.ConsultationDoctorFollowUp;
import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationMessage;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorMessageAiLog;
import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
import cn.gugufish.entity.vo.response.ConsultationMessageSummaryVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationDoctorConclusionMapper;
import cn.gugufish.mapper.ConsultationDoctorFollowUpMapper;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.mapper.ConsultationMessageMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorMessageAiLogMapper;
import cn.gugufish.service.ConsultationMessageService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConsultationMessageServiceImpl implements ConsultationMessageService {

    @Resource
    ConsultationMessageMapper consultationMessageMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Resource
    ConsultationDoctorHandleMapper consultationDoctorHandleMapper;

    @Resource
    ConsultationDoctorConclusionMapper consultationDoctorConclusionMapper;

    @Resource
    ConsultationDoctorFollowUpMapper consultationDoctorFollowUpMapper;

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    DoctorMessageAiLogMapper doctorMessageAiLogMapper;

    @Resource
    DepartmentMapper departmentMapper;

    @Override
    public List<ConsultationMessageVO> listUserMessages(int accountId, int recordId) {
        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", recordId)
                .eq("account_id", accountId));
        if (record == null) return null;
        markMessagesRead(recordId, "user");
        return listMessages(recordId);
    }

    @Override
    public List<ConsultationMessageVO> listDoctorMessages(int accountId, int recordId) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return null;

        ConsultationRecord record = consultationRecordMapper.selectById(recordId);
        if (record == null || !Objects.equals(doctor.getDepartmentId(), record.getDepartmentId())) return null;
        markMessagesRead(recordId, "doctor");
        return listMessages(recordId);
    }

    @Override
    @Transactional
    public String sendUserMessage(int accountId, ConsultationMessageSendVO vo) {
        String sceneType = normalizeMessageSceneType(vo == null ? null : vo.getSceneType());
        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", vo.getRecordId())
                .eq("account_id", accountId));
        if (record == null) return "问诊记录不存在或暂无发送权限";
        if (!hasMessagePayload(vo)) return "请至少填写消息内容或上传图片附件";

        if ("followup_update".equals(sceneType) && !canSendFollowUpUpdate(record)) {
            return "褰撳墠闂瘖鏆傛棤寰呴殢璁夸簨椤癸紝璇峰厛鍦ㄦ櫘閫氭矡閫氬尯琛ュ厖鎯呭喌";
        }

        if ("check_result_update".equals(sceneType) && !canSendCheckResultUpdate(record)) {
            return "当前问诊尚未进入可补充检查结果的阶段，可先通过普通留言补充资料";
        }

        ConsultationMessage message = saveMessage(
                record.getId(),
                "user",
                accountId,
                record.getPatientName(),
                "用户",
                vo,
                new Date()
        );
        return message == null ? "问诊消息发送失败" : null;
    }

    @Override
    @Transactional
    public String sendDoctorMessage(int accountId, ConsultationMessageSendVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";
        if (!hasMessagePayload(vo)) return "请至少填写消息内容或上传图片附件";

        ConsultationRecord record = consultationRecordMapper.selectById(vo.getRecordId());
        if (record == null) return "问诊记录不存在";
        if (!Objects.equals(doctor.getDepartmentId(), record.getDepartmentId())) return "当前问诊记录不属于你所在科室";

        ConsultationDoctorHandle handle = findHandle(vo.getRecordId());
        if (handle != null && handle.getDoctorId() != null && !Objects.equals(handle.getDoctorId(), doctor.getId())) {
            return "该问诊单已由医生 " + handle.getDoctorName() + " 进入处理流程";
        }

        Date now = new Date();
        String departmentName = resolveDepartmentName(doctor, record);
        String claimMessage = ensureClaimed(vo.getRecordId(), doctor, departmentName, now);
        if (claimMessage != null) return claimMessage;

        ConsultationMessage message = saveMessage(
                record.getId(),
                "doctor",
                doctor.getId(),
                doctor.getName(),
                trimToNull(doctor.getTitle()) == null ? "医生" : doctor.getTitle(),
                vo,
                now
        );
        if (message == null) return "问诊消息发送失败";
        String processingMessage = syncDoctorProcessingState(record, handle, doctor, departmentName, now);
        if (processingMessage != null) return processingMessage;
        markDoctorAiLogSent(vo, doctor, record, message, now);
        return null;
    }

    @Override
    public Map<Integer, ConsultationMessageSummaryVO> summarizeDoctorMessages(List<Integer> consultationIds) {
        return summarizeMessages(consultationIds, "doctor");
    }

    @Override
    public Map<Integer, ConsultationMessageSummaryVO> summarizeUserMessages(List<Integer> consultationIds) {
        return summarizeMessages(consultationIds, "user");
    }

    private List<ConsultationMessageVO> listMessages(int consultationId) {
        return consultationMessageMapper.selectList(Wrappers.<ConsultationMessage>query()
                        .eq("consultation_id", consultationId)
                        .eq("status", 1)
                        .orderByAsc("create_time")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationMessageVO.class, vo -> vo.setAttachments(parseAttachments(item.getAttachmentsJson()))))
                .toList();
    }

    private void markMessagesRead(int consultationId, String viewerType) {
        String senderType = unreadSenderType(viewerType);
        if (senderType == null) return;
        Date now = new Date();
        consultationMessageMapper.update(null, Wrappers.<ConsultationMessage>update()
                .eq("consultation_id", consultationId)
                .eq("status", 1)
                .eq("sender_type", senderType)
                .and(wrapper -> wrapper.ne("read_status", 1).or().isNull("read_status"))
                .set("read_status", 1)
                .set("read_time", now)
                .set("update_time", now));
    }

    private Map<Integer, ConsultationMessageSummaryVO> summarizeMessages(List<Integer> consultationIds, String viewerType) {
        if (consultationIds == null || consultationIds.isEmpty()) return Map.of();

        List<Integer> validIds = consultationIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (validIds.isEmpty()) return Map.of();

        Map<Integer, ConsultationMessageSummaryVO> summaryMap = new HashMap<>();
        validIds.forEach(id -> summaryMap.put(id, createEmptySummary()));

        consultationMessageMapper.selectList(Wrappers.<ConsultationMessage>query()
                        .in("consultation_id", validIds)
                        .eq("status", 1)
                        .orderByAsc("consultation_id")
                        .orderByAsc("create_time")
                        .orderByAsc("id"))
                .forEach(item -> {
                    ConsultationMessageSummaryVO summary = summaryMap.computeIfAbsent(item.getConsultationId(), key -> createEmptySummary());
                    summary.setTotalCount(defaultInt(summary.getTotalCount()) + 1);
                    if ("user".equals(item.getSenderType())) {
                        summary.setUserMessageCount(defaultInt(summary.getUserMessageCount()) + 1);
                        if (!Objects.equals(item.getReadStatus(), 1)) {
                            summary.setUnreadByDoctorCount(defaultInt(summary.getUnreadByDoctorCount()) + 1);
                        }
                    } else if ("doctor".equals(item.getSenderType())) {
                        summary.setDoctorMessageCount(defaultInt(summary.getDoctorMessageCount()) + 1);
                        if (!Objects.equals(item.getReadStatus(), 1)) {
                            summary.setUnreadByPatientCount(defaultInt(summary.getUnreadByPatientCount()) + 1);
                        }
                    }
                    if (isUnreadForViewer(item, viewerType)) {
                        summary.setUnreadCount(defaultInt(summary.getUnreadCount()) + 1);
                    }
                    summary.setLatestSenderType(item.getSenderType());
                    summary.setLatestSenderName(resolveSenderName(item));
                    summary.setLatestMessageType(item.getMessageType());
                    summary.setLatestMessagePreview(buildMessagePreview(item));
                    summary.setLatestTime(item.getCreateTime());
                });

        return summaryMap;
    }

    private ConsultationMessage saveMessage(int consultationId,
                                            String senderType,
                                            Integer senderId,
                                            String senderName,
                                            String senderRoleName,
                                            ConsultationMessageSendVO vo,
                                            Date now) {
        String content = trimToNull(vo.getContent());
        List<String> attachments = normalizeAttachments(vo.getAttachments());
        if (content == null && attachments.isEmpty()) return null;

        String messageType = resolveMessageType(vo, content, attachments);
        boolean autoRead = isAutoReadMessageType(messageType);

        ConsultationMessage message = new ConsultationMessage(
                null,
                consultationId,
                senderType,
                senderId,
                senderName,
                senderRoleName,
                messageType,
                content,
                attachments.isEmpty() ? null : JSON.toJSONString(attachments),
                1,
                autoRead ? 1 : 0,
                autoRead ? now : null,
                now,
                now
        );
        return consultationMessageMapper.insert(message) > 0 ? message : null;
    }

    private String resolveMessageType(ConsultationMessageSendVO vo, String content, List<String> attachments) {
        String sceneType = normalizeMessageSceneType(vo == null ? null : vo.getSceneType());
        if ("followup_update".equals(sceneType)) return "followup_update";
        if ("check_result_update".equals(sceneType)) return "check_result_update";
        if ("doctor_guidance_ack".equals(sceneType)) return "doctor_guidance_ack";
        return attachments == null || attachments.isEmpty()
                ? "text"
                : content == null ? "image" : "mixed";
    }

    private String normalizeMessageSceneType(String sceneType) {
        String normalized = trimToNull(sceneType);
        if (normalized == null) return null;
        normalized = normalized.toLowerCase().replace('-', '_');
        return switch (normalized) {
            case "follow_up_update", "followup_update", "recovery_update" -> "followup_update";
            case "check_result", "check_result_update", "report_update", "report_upload", "inspection_result", "result_update" -> "check_result_update";
            case "doctor_guidance_ack", "guidance_ack", "doctor_ack", "conclusion_ack", "advice_ack" -> "doctor_guidance_ack";
            default -> null;
        };
    }

    private boolean hasMessagePayload(ConsultationMessageSendVO vo) {
        if (vo == null) return false;
        return trimToNull(vo.getContent()) != null || !normalizeAttachments(vo.getAttachments()).isEmpty();
    }

    private void markDoctorAiLogSent(ConsultationMessageSendVO vo,
                                     Doctor doctor,
                                     ConsultationRecord record,
                                     ConsultationMessage message,
                                     Date now) {
        if (vo == null || vo.getAiLogId() == null || doctor == null || record == null || message == null) return;
        DoctorMessageAiLog logRecord = doctorMessageAiLogMapper.selectById(vo.getAiLogId());
        if (logRecord == null) return;
        if (!Objects.equals(logRecord.getDoctorId(), doctor.getId())) return;
        if (!Objects.equals(logRecord.getConsultationId(), record.getId())) return;

        logRecord.setSentStatus(1);
        logRecord.setSentMessageId(message.getId());
        logRecord.setSentContentPreview(abbreviateText(trimToNull(message.getContent()), 500));
        logRecord.setSentTime(now);
        logRecord.setUpdateTime(now);
        doctorMessageAiLogMapper.updateById(logRecord);
    }

    private List<String> parseAttachments(String attachmentsJson) {
        if (trimToNull(attachmentsJson) == null) return List.of();
        try {
            List<String> items = JSON.parseArray(attachmentsJson, String.class);
            return items == null ? List.of() : items.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> normalizeAttachments(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) return List.of();
        return attachments.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .limit(6)
                .toList();
    }

    private boolean isUnreadForViewer(ConsultationMessage message, String viewerType) {
        String senderType = unreadSenderType(viewerType);
        return senderType != null
                && senderType.equals(message.getSenderType())
                && !Objects.equals(message.getReadStatus(), 1);
    }

    private String unreadSenderType(String viewerType) {
        return switch (trimToNull(viewerType) == null ? "" : viewerType.trim().toLowerCase()) {
            case "doctor" -> "user";
            case "user" -> "doctor";
            default -> null;
        };
    }

    private ConsultationMessageSummaryVO createEmptySummary() {
        ConsultationMessageSummaryVO summary = new ConsultationMessageSummaryVO();
        summary.setTotalCount(0);
        summary.setUserMessageCount(0);
        summary.setDoctorMessageCount(0);
        summary.setUnreadCount(0);
        summary.setUnreadByDoctorCount(0);
        summary.setUnreadByPatientCount(0);
        return summary;
    }

    private String resolveSenderName(ConsultationMessage message) {
        String senderName = trimToNull(message.getSenderName());
        if (senderName != null) return senderName;
        return "doctor".equals(message.getSenderType()) ? "医生" : "患者";
    }

    private String buildMessagePreview(ConsultationMessage message) {
        String content = trimToNull(message.getContent());
        int attachmentCount = parseAttachments(message.getAttachmentsJson()).size();
        String imageSuffix = attachmentCount <= 0
                ? null
                : attachmentCount == 1 ? "[图片]" : "[图片 x" + attachmentCount + "]";

        String preview;
        if (content != null && imageSuffix != null) {
            preview = abbreviateText(content + " " + imageSuffix, 72);
        } else if (content != null) {
            preview = abbreviateText(content, 72);
        } else if (imageSuffix != null) {
            preview = imageSuffix;
        } else {
            preview = "[消息]";
        }
        String messageType = trimToNull(message.getMessageType());
        if (isDoctorGuidanceAckMessageType(messageType)) {
            return preview.startsWith("[已确认查看]") || preview.startsWith("[Acknowledged]")
                    ? preview
                    : abbreviateText("[已确认查看] " + preview, 72);
        }
        if ("check_result_update".equals(messageType)) {
            return preview.startsWith("[检查结果]") || preview.startsWith("[Check Result]")
                    ? preview
                    : abbreviateText("[检查结果] " + preview, 72);
        }
        if (!"followup_update".equals(messageType) && !"check_result_update".equals(messageType)) return preview;
        if (preview.startsWith("[恢复更新]") || preview.startsWith("[Recovery Update]")) return preview;
        return abbreviateText("[恢复更新] " + preview, 72);
    }

    private String abbreviateText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(maxLength - 3, 0)) + "...";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isAutoReadMessageType(String messageType) {
        return isDoctorGuidanceAckMessageType(messageType);
    }

    private boolean isDoctorGuidanceAckMessageType(String messageType) {
        return "doctor_guidance_ack".equals(trimToNull(messageType));
    }

    private Doctor validDoctor(int accountId) {
        Doctor doctor = doctorMapper.selectOne(Wrappers.<Doctor>query()
                .eq("account_id", accountId)
                .last("limit 1"));
        if (doctor == null) return null;
        if (doctor.getStatus() == null || doctor.getStatus() != 1) return null;
        if (doctor.getDepartmentId() == null) return null;
        return doctor;
    }

    private ConsultationDoctorAssignment findAssignment(int consultationId) {
        return consultationDoctorAssignmentMapper.selectOne(Wrappers.<ConsultationDoctorAssignment>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
    }

    private ConsultationDoctorHandle findHandle(int consultationId) {
        return consultationDoctorHandleMapper.selectOne(Wrappers.<ConsultationDoctorHandle>query()
                .eq("consultation_id", consultationId)
                .last("limit 1"));
    }

    private ConsultationDoctorConclusion findConclusion(int consultationId) {
        return consultationDoctorConclusionMapper.selectOne(Wrappers.<ConsultationDoctorConclusion>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("update_time")
                .orderByDesc("id")
                .last("limit 1"));
    }

    private ConsultationDoctorFollowUp findLatestFollowUp(int consultationId) {
        return consultationDoctorFollowUpMapper.selectOne(Wrappers.<ConsultationDoctorFollowUp>query()
                .eq("consultation_id", consultationId)
                .orderByDesc("create_time")
                .orderByDesc("id")
                .last("limit 1"));
    }

    private boolean canSendFollowUpUpdate(ConsultationRecord record) {
        if (record == null) return false;
        ConsultationDoctorHandle handle = findHandle(record.getId());
        if (handle == null || !"completed".equals(handle.getStatus())) return false;
        ConsultationDoctorConclusion conclusion = findConclusion(record.getId());
        if (conclusion == null || !Objects.equals(conclusion.getNeedFollowUp(), 1)) return false;
        ConsultationDoctorFollowUp latestFollowUp = findLatestFollowUp(record.getId());
        return latestFollowUp == null || !Objects.equals(latestFollowUp.getNeedRevisit(), 0);
    }

    private boolean canSendCheckResultUpdate(ConsultationRecord record) {
        if (record == null) return false;
        String status = trimToNull(record.getStatus());
        if ("triaged".equals(status) || "processing".equals(status) || "completed".equals(status)) return true;
        return trimToNull(record.getTriageLevelName()) != null;
    }

    private String ensureClaimed(int consultationId, Doctor doctor, String departmentName, Date now) {
        ConsultationDoctorAssignment assignment = findAssignment(consultationId);
        if (assignment == null) {
            assignment = new ConsultationDoctorAssignment(
                    null,
                    consultationId,
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getDepartmentId(),
                    departmentName,
                    "claimed",
                    now,
                    null,
                    now,
                    now
            );
            return consultationDoctorAssignmentMapper.insert(assignment) > 0 ? null : "问诊认领失败";
        }

        if ("claimed".equals(assignment.getStatus()) && Objects.equals(doctor.getId(), assignment.getDoctorId())) {
            return null;
        }
        if ("claimed".equals(assignment.getStatus()) && !Objects.equals(doctor.getId(), assignment.getDoctorId())) {
            return "当前问诊单已由医生 " + assignment.getDoctorName() + " 认领";
        }

        assignment.setDoctorId(doctor.getId());
        assignment.setDoctorName(doctor.getName());
        assignment.setDepartmentId(doctor.getDepartmentId());
        assignment.setDepartmentName(departmentName);
        assignment.setStatus("claimed");
        assignment.setClaimTime(now);
        assignment.setReleaseTime(null);
        assignment.setUpdateTime(now);
        return consultationDoctorAssignmentMapper.updateById(assignment) > 0 ? null : "问诊认领失败";
    }

    private String syncDoctorProcessingState(ConsultationRecord record,
                                             ConsultationDoctorHandle handle,
                                             Doctor doctor,
                                             String departmentName,
                                             Date now) {
        if (record == null || doctor == null || now == null) return null;
        if ("completed".equals(record.getStatus())) return null;
        if (handle != null && "completed".equals(handle.getStatus())) return null;

        if (handle == null) {
            ConsultationDoctorHandle processingHandle = new ConsultationDoctorHandle(
                    null,
                    record.getId(),
                    doctor.getId(),
                    doctor.getName(),
                    doctor.getDepartmentId(),
                    departmentName,
                    "processing",
                    null,
                    null,
                    null,
                    null,
                    now,
                    null,
                    now,
                    now
            );
            if (consultationDoctorHandleMapper.insert(processingHandle) <= 0) {
                return "问诊接诊状态更新失败";
            }
        } else {
            handle.setDoctorId(doctor.getId());
            handle.setDoctorName(doctor.getName());
            handle.setDepartmentId(doctor.getDepartmentId());
            handle.setDepartmentName(departmentName);
            if (!"completed".equals(handle.getStatus())) {
                handle.setStatus("processing");
            }
            if (handle.getReceiveTime() == null) {
                handle.setReceiveTime(now);
            }
            handle.setUpdateTime(now);
            if (consultationDoctorHandleMapper.updateById(handle) <= 0) {
                return "问诊接诊状态更新失败";
            }
        }

        if (!"processing".equals(record.getStatus())) {
            record.setStatus("processing");
            record.setUpdateTime(now);
            if (consultationRecordMapper.updateById(record) <= 0) {
                return "问诊状态更新失败";
            }
        }
        return null;
    }

    private String resolveDepartmentName(Doctor doctor, ConsultationRecord record) {
        Department department = departmentMapper.selectById(doctor.getDepartmentId());
        return department == null ? record.getDepartmentName() : department.getName();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
