package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Account;
import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationPayment;
import cn.gugufish.entity.dto.ConsultationPrescription;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.OperationLog;
import cn.gugufish.entity.dto.PatientMedicalHistory;
import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.vo.request.AdminAccountPasswordResetVO;
import cn.gugufish.entity.vo.request.AdminAccountRoleUpdateVO;
import cn.gugufish.entity.vo.response.AdminAccountManageVO;
import cn.gugufish.entity.vo.response.AdminOperationLogVO;
import cn.gugufish.entity.vo.response.AdminOrderManageVO;
import cn.gugufish.entity.vo.response.AdminUserManageVO;
import cn.gugufish.mapper.AccountMapper;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationPaymentMapper;
import cn.gugufish.mapper.ConsultationPrescriptionMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.OperationLogMapper;
import cn.gugufish.mapper.PatientMedicalHistoryMapper;
import cn.gugufish.mapper.PatientProfileMapper;
import cn.gugufish.service.AdminSystemService;
import cn.gugufish.utils.Const;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminSystemServiceImpl implements AdminSystemService {

    @Resource
    AccountMapper accountMapper;

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    PatientProfileMapper patientProfileMapper;

    @Resource
    PatientMedicalHistoryMapper patientMedicalHistoryMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationPaymentMapper consultationPaymentMapper;

    @Resource
    ConsultationPrescriptionMapper consultationPrescriptionMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Resource
    OperationLogMapper operationLogMapper;

    @Resource
    PasswordEncoder passwordEncoder;

    @Override
    public List<AdminAccountManageVO> listAccounts() {
        List<Account> accounts = accountMapper.selectList(Wrappers.<Account>query()
                .orderByDesc("register_time")
                .orderByDesc("id"));
        if (accounts.isEmpty()) return List.of();

        List<Integer> accountIds = accounts.stream().map(Account::getId).toList();
        Map<Integer, Doctor> doctorMap = new HashMap<>();
        doctorMapper.selectList(Wrappers.<Doctor>query()
                        .in("account_id", accountIds))
                .forEach(item -> doctorMap.put(item.getAccountId(), item));

        Map<Integer, Integer> patientCountMap = countByAccount(patientProfileMapper.selectList(Wrappers.<PatientProfile>query()
                .in("account_id", accountIds)), PatientProfile::getAccountId);
        Map<Integer, Integer> consultationCountMap = countByAccount(consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .in("account_id", accountIds)), ConsultationRecord::getAccountId);
        Map<Integer, Integer> paymentCountMap = countByAccount(consultationPaymentMapper.selectList(Wrappers.<ConsultationPayment>query()
                .in("account_id", accountIds)), ConsultationPayment::getAccountId);

        List<AdminAccountManageVO> result = new ArrayList<>();
        for (Account account : accounts) {
            Doctor doctor = doctorMap.get(account.getId());
            AdminAccountManageVO vo = account.asViewObject(AdminAccountManageVO.class);
            vo.setDoctorId(doctor == null ? null : doctor.getId());
            vo.setDoctorName(doctor == null ? null : doctor.getName());
            vo.setPatientCount(patientCountMap.getOrDefault(account.getId(), 0));
            vo.setConsultationCount(consultationCountMap.getOrDefault(account.getId(), 0));
            vo.setPaymentCount(paymentCountMap.getOrDefault(account.getId(), 0));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<AdminUserManageVO> listUsers() {
        List<Account> users = accountMapper.selectList(Wrappers.<Account>query()
                .eq("role", Const.ROLE_DEFAULT)
                .orderByDesc("register_time")
                .orderByDesc("id"));
        if (users.isEmpty()) return List.of();

        List<Integer> accountIds = users.stream().map(Account::getId).toList();
        List<PatientProfile> patientProfiles = patientProfileMapper.selectList(Wrappers.<PatientProfile>query()
                .in("account_id", accountIds));
        Map<Integer, Integer> patientCountMap = countByAccount(patientProfiles, PatientProfile::getAccountId);

        Map<Integer, Integer> patientAccountMap = new HashMap<>();
        List<Integer> patientIds = new ArrayList<>();
        for (PatientProfile profile : patientProfiles) {
            patientIds.add(profile.getId());
            patientAccountMap.put(profile.getId(), profile.getAccountId());
        }

        Map<Integer, Integer> medicalHistoryCountMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            List<PatientMedicalHistory> histories = patientMedicalHistoryMapper.selectList(Wrappers.<PatientMedicalHistory>query()
                    .in("patient_id", patientIds));
            for (PatientMedicalHistory item : histories) {
                Integer accountId = patientAccountMap.get(item.getPatientId());
                if (accountId == null) continue;
                medicalHistoryCountMap.put(accountId, medicalHistoryCountMap.getOrDefault(accountId, 0) + 1);
            }
        }

        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .in("account_id", accountIds)
                .orderByDesc("create_time")
                .orderByDesc("id"));
        Map<Integer, Integer> consultationCountMap = new HashMap<>();
        Map<Integer, Integer> completedConsultationCountMap = new HashMap<>();
        Map<Integer, Date> latestConsultationTimeMap = new HashMap<>();
        Map<Integer, ConsultationRecord> consultationRecordMap = new LinkedHashMap<>();
        for (ConsultationRecord item : records) {
            consultationCountMap.put(item.getAccountId(), consultationCountMap.getOrDefault(item.getAccountId(), 0) + 1);
            if ("completed".equalsIgnoreCase(trimToNull(item.getStatus()))) {
                completedConsultationCountMap.put(item.getAccountId(), completedConsultationCountMap.getOrDefault(item.getAccountId(), 0) + 1);
            }
            latestConsultationTimeMap.putIfAbsent(item.getAccountId(), item.getUpdateTime() == null ? item.getCreateTime() : item.getUpdateTime());
            consultationRecordMap.put(item.getId(), item);
        }

        Map<Integer, Integer> paidOrderCountMap = new HashMap<>();
        consultationPaymentMapper.selectList(Wrappers.<ConsultationPayment>query()
                        .in("account_id", accountIds)
                        .eq("status", "paid"))
                .forEach(item -> paidOrderCountMap.put(item.getAccountId(), paidOrderCountMap.getOrDefault(item.getAccountId(), 0) + 1));

        Map<Integer, Integer> prescriptionCountMap = new HashMap<>();
        if (!consultationRecordMap.isEmpty()) {
            consultationPrescriptionMapper.selectList(Wrappers.<ConsultationPrescription>query()
                            .in("consultation_id", consultationRecordMap.keySet()))
                    .forEach(item -> {
                        ConsultationRecord record = consultationRecordMap.get(item.getConsultationId());
                        if (record == null) return;
                        Integer accountId = record.getAccountId();
                        prescriptionCountMap.put(accountId, prescriptionCountMap.getOrDefault(accountId, 0) + 1);
                    });
        }

        return users.stream().map(item -> {
            AdminUserManageVO vo = new AdminUserManageVO();
            vo.setAccountId(item.getId());
            vo.setUsername(item.getUsername());
            vo.setEmail(item.getEmail());
            vo.setRegisterTime(item.getRegisterTime());
            vo.setPatientCount(patientCountMap.getOrDefault(item.getId(), 0));
            vo.setMedicalHistoryCount(medicalHistoryCountMap.getOrDefault(item.getId(), 0));
            vo.setConsultationCount(consultationCountMap.getOrDefault(item.getId(), 0));
            vo.setCompletedConsultationCount(completedConsultationCountMap.getOrDefault(item.getId(), 0));
            vo.setPaidOrderCount(paidOrderCountMap.getOrDefault(item.getId(), 0));
            vo.setPrescriptionCount(prescriptionCountMap.getOrDefault(item.getId(), 0));
            vo.setLatestConsultationTime(latestConsultationTimeMap.get(item.getId()));
            return vo;
        }).toList();
    }

    @Override
    public List<AdminOrderManageVO> listOrders() {
        List<ConsultationPayment> payments = consultationPaymentMapper.selectList(Wrappers.<ConsultationPayment>query()
                .orderByDesc("update_time")
                .orderByDesc("id"));
        if (payments.isEmpty()) return List.of();

        List<Integer> accountIds = payments.stream().map(ConsultationPayment::getAccountId).distinct().toList();
        List<Integer> consultationIds = payments.stream().map(ConsultationPayment::getConsultationId).distinct().toList();

        Map<Integer, Account> accountMap = new HashMap<>();
        accountMapper.selectBatchIds(accountIds).forEach(item -> accountMap.put(item.getId(), item));

        Map<Integer, ConsultationRecord> recordMap = new HashMap<>();
        consultationRecordMapper.selectBatchIds(consultationIds).forEach(item -> recordMap.put(item.getId(), item));

        Map<Integer, ConsultationDoctorAssignment> assignmentMap = new HashMap<>();
        consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> assignmentMap.putIfAbsent(item.getConsultationId(), item));

        return payments.stream().map(item -> {
            Account account = accountMap.get(item.getAccountId());
            ConsultationRecord record = recordMap.get(item.getConsultationId());
            ConsultationDoctorAssignment assignment = assignmentMap.get(item.getConsultationId());
            AdminOrderManageVO vo = new AdminOrderManageVO();
            vo.setId(item.getId());
            vo.setPaymentNo(item.getPaymentNo());
            vo.setConsultationId(item.getConsultationId());
            vo.setConsultationNo(record == null ? null : record.getConsultationNo());
            vo.setConsultationStatus(record == null ? null : record.getStatus());
            vo.setAccountId(item.getAccountId());
            vo.setUsername(account == null ? null : account.getUsername());
            vo.setPatientId(item.getPatientId());
            vo.setPatientName(item.getPatientName());
            vo.setCategoryName(item.getCategoryName());
            vo.setDoctorName(assignment == null ? null : assignment.getDoctorName());
            vo.setDepartmentName(assignment == null ? null : assignment.getDepartmentName());
            vo.setAmount(item.getAmount());
            vo.setStatus(item.getStatus());
            vo.setPaymentChannel(item.getPaymentChannel());
            vo.setPaidTime(item.getPaidTime());
            vo.setCreateTime(item.getCreateTime());
            vo.setUpdateTime(item.getUpdateTime());
            return vo;
        }).toList();
    }

    @Override
    public List<AdminOperationLogVO> listOperationLogs() {
        return operationLogMapper.selectList(Wrappers.<OperationLog>query()
                        .orderByDesc("id")
                        .last("limit 2000"))
                .stream()
                .map(item -> item.asViewObject(AdminOperationLogVO.class))
                .toList();
    }

    @Override
    public String updateAccountRole(AdminAccountRoleUpdateVO vo) {
        Account account = accountMapper.selectById(vo.getId());
        if (account == null) return "当前账号不存在";
        String targetRole = trimToNull(vo.getRole());
        if (targetRole == null) return "账号角色不能为空";
        if (Objects.equals(account.getRole(), targetRole)) return null;

        Doctor boundDoctor = doctorMapper.selectOne(Wrappers.<Doctor>query()
                .eq("account_id", account.getId())
                .last("limit 1"));
        if (boundDoctor != null && !Const.ROLE_DOCTOR.equals(targetRole)) {
            return "该账号已绑定医生档案，请先解绑医生后再改成非 doctor 角色";
        }

        boolean updated = accountMapper.update(null, Wrappers.<Account>update()
                .eq("id", vo.getId())
                .set("role", targetRole)) > 0;
        return updated ? null : "账号角色更新失败，请稍后重试";
    }

    @Override
    public String resetAccountPassword(AdminAccountPasswordResetVO vo) {
        Account account = accountMapper.selectById(vo.getId());
        if (account == null) return "当前账号不存在";
        boolean updated = accountMapper.update(null, Wrappers.<Account>update()
                .eq("id", vo.getId())
                .set("password", passwordEncoder.encode(vo.getPassword()))) > 0;
        return updated ? null : "账号密码重置失败，请稍后重试";
    }

    private <T> Map<Integer, Integer> countByAccount(List<T> list, java.util.function.Function<T, Integer> classifier) {
        Map<Integer, Integer> result = new HashMap<>();
        for (T item : list) {
            Integer key = classifier.apply(item);
            if (key == null) continue;
            result.put(key, result.getOrDefault(key, 0) + 1);
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
