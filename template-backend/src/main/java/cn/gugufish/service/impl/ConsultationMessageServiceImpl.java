package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationDoctorHandle;
import cn.gugufish.entity.dto.ConsultationMessage;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.vo.request.ConsultationMessageSendVO;
import cn.gugufish.entity.vo.response.ConsultationMessageVO;
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
import java.util.List;
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
        return record == null ? null : listMessages(recordId);
    }

    @Override
    public List<ConsultationMessageVO> listDoctorMessages(int accountId, int recordId) {
        Doctor doctor = validDoctor(accountId);
        if (doctor == null) return null;

        ConsultationRecord record = consultationRecordMapper.selectById(recordId);
        if (record == null || !Objects.equals(doctor.getDepartmentId(), record.getDepartmentId())) return null;
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
