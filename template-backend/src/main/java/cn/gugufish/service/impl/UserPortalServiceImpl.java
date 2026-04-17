package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationPrescription;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.vo.response.DoctorDirectoryVO;
import cn.gugufish.entity.vo.response.UserPrescriptionVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationPrescriptionMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.service.UserPortalService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserPortalServiceImpl implements UserPortalService {

    private static final SimpleDateFormat SCHEDULE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    DoctorServiceTagMapper doctorServiceTagMapper;

    @Resource
    DoctorScheduleMapper doctorScheduleMapper;

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationPrescriptionMapper consultationPrescriptionMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Override
    public List<DoctorDirectoryVO> listDoctors() {
        List<Doctor> doctors = doctorMapper.selectList(Wrappers.<Doctor>query()
                .eq("status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        if (doctors.isEmpty()) return List.of();

        List<Integer> doctorIds = doctors.stream().map(Doctor::getId).toList();
        Map<Integer, Department> departmentMap = new HashMap<>();
        departmentMapper.selectList(Wrappers.<Department>query().eq("status", 1))
                .forEach(item -> departmentMap.put(item.getId(), item));

        Map<Integer, List<String>> tagMap = new HashMap<>();
        doctorServiceTagMapper.selectList(Wrappers.<DoctorServiceTag>query()
                        .in("doctor_id", doctorIds)
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .forEach(item -> tagMap.computeIfAbsent(item.getDoctorId(), key -> new ArrayList<>()).add(item.getTagName()));

        Date now = new Date();
        Map<Integer, DoctorSchedule> nextScheduleMap = new HashMap<>();
        Map<Integer, Integer> availableScheduleCountMap = new HashMap<>();
        doctorScheduleMapper.selectList(Wrappers.<DoctorSchedule>query()
                        .in("doctor_id", doctorIds)
                        .eq("status", 1)
                        .ge("schedule_date", now)
                        .orderByAsc("schedule_date")
                        .orderByAsc("id"))
                .forEach(item -> {
                    nextScheduleMap.putIfAbsent(item.getDoctorId(), item);
                    availableScheduleCountMap.put(item.getDoctorId(), availableScheduleCountMap.getOrDefault(item.getDoctorId(), 0) + 1);
                });

        return doctors.stream().map(item -> {
            DoctorDirectoryVO vo = new DoctorDirectoryVO();
            vo.setId(item.getId());
            vo.setDepartmentId(item.getDepartmentId());
            Department department = departmentMap.get(item.getDepartmentId());
            vo.setDepartmentName(department == null ? null : department.getName());
            vo.setName(item.getName());
            vo.setTitle(item.getTitle());
            vo.setPhoto(item.getPhoto());
            vo.setIntroduction(item.getIntroduction());
            vo.setExpertise(item.getExpertise());
            vo.setStatus(item.getStatus());
            vo.setAvailableScheduleCount(availableScheduleCountMap.getOrDefault(item.getId(), 0));
            vo.setNextScheduleText(renderScheduleText(nextScheduleMap.get(item.getId())));
            vo.setUpdateTime(item.getUpdateTime());
            vo.setServiceTags(tagMap.getOrDefault(item.getId(), List.of()));
            return vo;
        }).toList();
    }

    @Override
    public List<UserPrescriptionVO> listPrescriptions(int accountId) {
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .eq("account_id", accountId)
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) return List.of();

        Map<Integer, ConsultationRecord> recordMap = new LinkedHashMap<>();
        records.forEach(item -> recordMap.put(item.getId(), item));
        List<Integer> consultationIds = new ArrayList<>(recordMap.keySet());

        Map<Integer, ConsultationDoctorAssignment> assignmentMap = new HashMap<>();
        consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .forEach(item -> assignmentMap.putIfAbsent(item.getConsultationId(), item));

        return consultationPrescriptionMapper.selectList(Wrappers.<ConsultationPrescription>query()
                        .in("consultation_id", consultationIds)
                        .orderByDesc("create_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> {
                    ConsultationRecord record = recordMap.get(item.getConsultationId());
                    ConsultationDoctorAssignment assignment = assignmentMap.get(item.getConsultationId());
                    UserPrescriptionVO vo = new UserPrescriptionVO();
                    vo.setId(item.getId());
                    vo.setConsultationId(item.getConsultationId());
                    vo.setConsultationNo(record == null ? null : record.getConsultationNo());
                    vo.setConsultationStatus(record == null ? null : record.getStatus());
                    vo.setPatientId(record == null ? null : record.getPatientId());
                    vo.setPatientName(record == null ? null : record.getPatientName());
                    vo.setConsultationCategoryName(record == null ? null : record.getCategoryName());
                    vo.setDoctorName(assignment == null ? null : assignment.getDoctorName());
                    vo.setMedicineId(item.getMedicineId());
                    vo.setMedicineName(item.getMedicineName());
                    vo.setGenericName(item.getGenericName());
                    vo.setMedicineCategoryName(item.getCategoryName());
                    vo.setSpecification(item.getSpecification());
                    vo.setDosage(item.getDosage());
                    vo.setFrequency(item.getFrequency());
                    vo.setDurationDays(item.getDurationDays());
                    vo.setMedicationInstruction(item.getMedicationInstruction());
                    vo.setWarningSummary(item.getWarningSummary());
                    vo.setCreateTime(item.getCreateTime());
                    vo.setUpdateTime(item.getUpdateTime());
                    return vo;
                })
                .toList();
    }

    private String renderScheduleText(DoctorSchedule schedule) {
        if (schedule == null || schedule.getScheduleDate() == null) return "暂未配置后续排班";
        return SCHEDULE_DATE_FORMAT.format(schedule.getScheduleDate()) + " " + renderTimePeriod(schedule.getTimePeriod());
    }

    private String renderTimePeriod(String value) {
        if (value == null) return "排班";
        return switch (value.trim().toLowerCase()) {
            case "morning" -> "上午";
            case "afternoon" -> "下午";
            case "evening" -> "晚间";
            default -> value;
        };
    }
}
