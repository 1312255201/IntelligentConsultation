package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.PatientMedicalHistory;
import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.vo.request.PatientMedicalHistorySaveVO;
import cn.gugufish.mapper.PatientMedicalHistoryMapper;
import cn.gugufish.mapper.PatientProfileMapper;
import cn.gugufish.service.PatientMedicalHistoryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PatientMedicalHistoryServiceImpl extends ServiceImpl<PatientMedicalHistoryMapper, PatientMedicalHistory> implements PatientMedicalHistoryService {

    @Resource
    PatientProfileMapper patientProfileMapper;

    @Override
    public List<PatientMedicalHistory> listByAccountId(int accountId) {
        List<Integer> patientIds = patientProfileMapper.selectList(Wrappers.<PatientProfile>query()
                        .eq("account_id", accountId)
                        .select("id"))
                .stream()
                .map(PatientProfile::getId)
                .toList();
        if (patientIds.isEmpty()) return List.of();
        return this.list(Wrappers.<PatientMedicalHistory>query()
                .in("patient_id", patientIds)
                .orderByDesc("update_time")
                .orderByAsc("id"));
    }

    @Override
    public PatientMedicalHistory getByPatientId(int accountId, int patientId) {
        return ownedPatient(accountId, patientId) == null ? null : this.getOne(Wrappers.<PatientMedicalHistory>query()
                .eq("patient_id", patientId), false);
    }

    @Override
    public String saveHistory(int accountId, PatientMedicalHistorySaveVO vo) {
        if (ownedPatient(accountId, vo.getPatientId()) == null) {
            return "就诊人不存在";
        }
        if (isEmptyHistory(vo)) {
            return "请至少完善一项健康档案信息后再保存";
        }

        PatientMedicalHistory current = this.getOne(Wrappers.<PatientMedicalHistory>query()
                .eq("patient_id", vo.getPatientId()), false);
        Date now = new Date();
        if (current == null) {
            PatientMedicalHistory history = new PatientMedicalHistory(
                    null,
                    vo.getPatientId(),
                    blankToNull(vo.getAllergyHistory()),
                    blankToNull(vo.getPastHistory()),
                    blankToNull(vo.getChronicHistory()),
                    blankToNull(vo.getSurgeryHistory()),
                    blankToNull(vo.getFamilyHistory()),
                    blankToNull(vo.getMedicationHistory()),
                    defaultPregnancyStatus(vo.getPregnancyStatus()),
                    defaultLactationStatus(vo.getLactationStatus()),
                    blankToNull(vo.getInfectiousHistory()),
                    now,
                    now
            );
            return this.save(history) ? null : "健康档案保存失败，请联系管理员";
        }

        boolean updated = this.update(Wrappers.<PatientMedicalHistory>update()
                .eq("patient_id", vo.getPatientId())
                .set("allergy_history", blankToNull(vo.getAllergyHistory()))
                .set("past_history", blankToNull(vo.getPastHistory()))
                .set("chronic_history", blankToNull(vo.getChronicHistory()))
                .set("surgery_history", blankToNull(vo.getSurgeryHistory()))
                .set("family_history", blankToNull(vo.getFamilyHistory()))
                .set("medication_history", blankToNull(vo.getMedicationHistory()))
                .set("pregnancy_status", defaultPregnancyStatus(vo.getPregnancyStatus()))
                .set("lactation_status", defaultLactationStatus(vo.getLactationStatus()))
                .set("infectious_history", blankToNull(vo.getInfectiousHistory()))
                .set("update_time", now));
        return updated ? null : "健康档案更新失败，请联系管理员";
    }

    @Override
    public String deleteHistory(int accountId, int patientId) {
        if (ownedPatient(accountId, patientId) == null) {
            return "就诊人不存在";
        }

        PatientMedicalHistory current = this.getOne(Wrappers.<PatientMedicalHistory>query()
                .eq("patient_id", patientId), false);
        if (current == null) return "该就诊人尚未建立健康档案";
        return this.removeById(current.getId()) ? null : "健康档案删除失败，请联系管理员";
    }

    private PatientProfile ownedPatient(int accountId, int patientId) {
        return patientProfileMapper.selectOne(Wrappers.<PatientProfile>query()
                .eq("id", patientId)
                .eq("account_id", accountId));
    }

    private boolean isEmptyHistory(PatientMedicalHistorySaveVO vo) {
        return isBlank(vo.getAllergyHistory())
                && isBlank(vo.getPastHistory())
                && isBlank(vo.getChronicHistory())
                && isBlank(vo.getSurgeryHistory())
                && isBlank(vo.getFamilyHistory())
                && isBlank(vo.getMedicationHistory())
                && isBlank(vo.getInfectiousHistory())
                && "unknown".equals(defaultPregnancyStatus(vo.getPregnancyStatus()))
                && "unknown".equals(defaultLactationStatus(vo.getLactationStatus()));
    }

    private String defaultPregnancyStatus(String value) {
        return isBlank(value) ? "unknown" : value;
    }

    private String defaultLactationStatus(String value) {
        return isBlank(value) ? "unknown" : value;
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
