package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationMessage;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
import cn.gugufish.entity.vo.response.ConsultationMessageSummaryVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationDoctorHandleMapper;
import cn.gugufish.mapper.ConsultationMessageMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
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
    DoctorMapper doctorMapper;

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
        ConsultationRecord record = consultationRecordMapper.selectOne(Wrappers.<ConsultationRecord>query()
                .eq("id", vo.getRecordId())
                .eq("account_id", accountId));
        if (record == null) return "问诊记录不存在或暂无发送权限";

        return saveMessage(
                record.getId(),
                "user",
                accountId,
                record.getPatientName(),
                "用户",
                vo,
                new Date()
        );
    }

    @Override
    @Transactional
    public String sendDoctorMessage(int accountId, ConsultationMessageSendVO vo) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

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

        return saveMessage(
                record.getId(),
                "doctor",
                doctor.getId(),
                doctor.getName(),
                trimToNull(doctor.getTitle()) == null ? "医生" : doctor.getTitle(),
                vo,
                now
        );
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
                    } else if ("doctor".equals(item.getSenderType())) {
                        summary.setDoctorMessageCount(defaultInt(summary.getDoctorMessageCount()) + 1);
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

    private String saveMessage(int consultationId,
                               String senderType,
                               Integer senderId,
                               String senderName,
                               String senderRoleName,
                               ConsultationMessageSendVO vo,
                               Date now) {
        String content = trimToNull(vo.getContent());
        List<String> attachments = normalizeAttachments(vo.getAttachments());
        if (content == null && attachments.isEmpty()) return "请至少填写消息内容或上传图片附件";

        String messageType = attachments.isEmpty()
                ? "text"
                : content == null ? "image" : "mixed";

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
                0,
                null,
                now,
                now
        );
        return consultationMessageMapper.insert(message) > 0 ? null : "问诊消息发送失败";
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

        if (content != null && imageSuffix != null) {
            return abbreviateText(content + " " + imageSuffix, 72);
        }
        if (content != null) return abbreviateText(content, 72);
        if (imageSuffix != null) return imageSuffix;
        return "[消息]";
    }

    private String abbreviateText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(maxLength - 3, 0)) + "...";
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
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
