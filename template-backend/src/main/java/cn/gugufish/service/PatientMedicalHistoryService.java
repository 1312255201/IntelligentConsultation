package cn.gugufish.service;

import cn.gugufish.entity.dto.PatientMedicalHistory;
import cn.gugufish.entity.vo.request.PatientMedicalHistorySaveVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PatientMedicalHistoryService extends IService<PatientMedicalHistory> {
    List<PatientMedicalHistory> listByAccountId(int accountId);
    PatientMedicalHistory getByPatientId(int accountId, int patientId);
    String saveHistory(int accountId, PatientMedicalHistorySaveVO vo);
    String deleteHistory(int accountId, int patientId);
}
