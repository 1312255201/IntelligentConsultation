package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.vo.response.ConsultationDispatchBatchCompareItemVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchBatchCompareVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchPreviewRecordVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchPreviewVO;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.service.ConsultationDispatchAdminService;
import cn.gugufish.service.ConsultationDispatchConfigService;
import cn.gugufish.service.ConsultationDoctorRecommendationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConsultationDispatchAdminServiceImpl implements ConsultationDispatchAdminService {

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationDoctorRecommendationService consultationDoctorRecommendationService;

    @Resource
    ConsultationDispatchConfigService consultationDispatchConfigService;

    @Override
    public List<ConsultationDispatchPreviewRecordVO> listPreviewRecords(int limit) {
        int previewLimit = Math.min(Math.max(limit, 1), 30);
        return consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                        .isNotNull("department_id")
                        .isNotNull("triage_action_type")
                        .in("status", List.of("triaged", "processing", "completed"))
                        .orderByDesc("create_time")
                        .orderByDesc("id")
                        .last("limit " + previewLimit))
                .stream()
                .map(item -> item.asViewObject(ConsultationDispatchPreviewRecordVO.class))
                .toList();
    }

    @Override
    public ConsultationDispatchPreviewVO preview(int consultationId, ConsultationDispatchConfig config) {
        ConsultationRecord record = consultationRecordMapper.selectById(consultationId);
        if (record == null) return null;
        return record.asViewObject(ConsultationDispatchPreviewVO.class, vo ->
                vo.setRecommendedDoctors(consultationDoctorRecommendationService.recommendDoctors(record, config)));
    }

    @Override
    public ConsultationDispatchBatchCompareVO batchPreview(List<Integer> consultationIds, ConsultationDispatchConfig currentConfig) {
        List<Integer> orderedIds = consultationIds == null ? List.of() : consultationIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(12)
                .toList();

        if (orderedIds.isEmpty()) {
            ConsultationDispatchBatchCompareVO emptyResult = new ConsultationDispatchBatchCompareVO();
            emptyResult.setTotalCount(0);
            emptyResult.setChangedCount(0);
            emptyResult.setUnchangedCount(0);
            emptyResult.setBothNoRecommendationCount(0);
            emptyResult.setItems(List.of());
            return emptyResult;
        }

        ConsultationDispatchConfig savedConfig = consultationDispatchConfigService.getConfig();
        Map<Integer, ConsultationRecord> recordMap = consultationRecordMapper.selectBatchIds(orderedIds).stream()
                .collect(Collectors.toMap(ConsultationRecord::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<ConsultationDispatchBatchCompareItemVO> items = orderedIds.stream()
                .map(recordMap::get)
                .filter(Objects::nonNull)
                .map(record -> buildBatchCompareItem(record, savedConfig, currentConfig))
                .toList();

        ConsultationDispatchBatchCompareVO result = new ConsultationDispatchBatchCompareVO();
        result.setTotalCount(items.size());
        result.setChangedCount((int) items.stream().filter(item -> Boolean.TRUE.equals(item.getTopDoctorChanged())).count());
        result.setBothNoRecommendationCount((int) items.stream().filter(item -> Boolean.TRUE.equals(item.getBothNoRecommendation())).count());
        result.setUnchangedCount(result.getTotalCount() - result.getChangedCount());
        result.setItems(items);
        return result;
    }

    private ConsultationDispatchBatchCompareItemVO buildBatchCompareItem(ConsultationRecord record,
                                                                         ConsultationDispatchConfig savedConfig,
                                                                         ConsultationDispatchConfig currentConfig) {
        List<ConsultationRecommendDoctorVO> savedDoctors = consultationDoctorRecommendationService.recommendDoctors(record, savedConfig);
        List<ConsultationRecommendDoctorVO> currentDoctors = consultationDoctorRecommendationService.recommendDoctors(record, currentConfig);
        ConsultationRecommendDoctorVO savedTopDoctor = savedDoctors.isEmpty() ? null : savedDoctors.get(0);
        ConsultationRecommendDoctorVO currentTopDoctor = currentDoctors.isEmpty() ? null : currentDoctors.get(0);

        return record.asViewObject(ConsultationDispatchBatchCompareItemVO.class, item -> {
            item.setConsultationId(record.getId());
            item.setSavedDoctorCount(savedDoctors.size());
            item.setCurrentDoctorCount(currentDoctors.size());
            item.setSharedDoctorCount(sharedDoctorCount(savedDoctors, currentDoctors));
            item.setTopDoctorChanged(isTopDoctorChanged(savedTopDoctor, currentTopDoctor));
            item.setBothNoRecommendation(savedTopDoctor == null && currentTopDoctor == null);
            fillTopDoctor(item, savedTopDoctor, true);
            fillTopDoctor(item, currentTopDoctor, false);
        });
    }

    private void fillTopDoctor(ConsultationDispatchBatchCompareItemVO item,
                               ConsultationRecommendDoctorVO doctor,
                               boolean saved) {
        if (doctor == null) return;
        if (saved) {
            item.setSavedTopDoctorId(doctor.getId());
            item.setSavedTopDoctorName(doctor.getName());
            item.setSavedTopDoctorTitle(doctor.getTitle());
            item.setSavedTopDoctorScore(doctor.getRecommendationScore());
        } else {
            item.setCurrentTopDoctorId(doctor.getId());
            item.setCurrentTopDoctorName(doctor.getName());
            item.setCurrentTopDoctorTitle(doctor.getTitle());
            item.setCurrentTopDoctorScore(doctor.getRecommendationScore());
        }
    }

    private boolean isTopDoctorChanged(ConsultationRecommendDoctorVO savedTopDoctor, ConsultationRecommendDoctorVO currentTopDoctor) {
        if (savedTopDoctor == null && currentTopDoctor == null) return false;
        if (savedTopDoctor == null || currentTopDoctor == null) return true;
        if (savedTopDoctor.getId() != null && currentTopDoctor.getId() != null) {
            return !Objects.equals(savedTopDoctor.getId(), currentTopDoctor.getId());
        }
        return !Objects.equals(savedTopDoctor.getName(), currentTopDoctor.getName());
    }

    private int sharedDoctorCount(List<ConsultationRecommendDoctorVO> savedDoctors, List<ConsultationRecommendDoctorVO> currentDoctors) {
        Set<String> currentKeys = currentDoctors.stream()
                .map(this::doctorKey)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toSet());
        return (int) savedDoctors.stream()
                .map(this::doctorKey)
                .filter(key -> !key.isEmpty())
                .filter(currentKeys::contains)
                .count();
    }

    private String doctorKey(ConsultationRecommendDoctorVO doctor) {
        if (doctor == null) return "";
        if (doctor.getId() != null) {
            return "id-" + doctor.getId();
        }
        return "name-" + Objects.toString(doctor.getName(), "");
    }
}
