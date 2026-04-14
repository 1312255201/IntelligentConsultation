package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationPrescription;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.MedicineCatalog;
import cn.gugufish.entity.dto.MedicineInteraction;
import cn.gugufish.entity.dto.MedicineWarning;
import cn.gugufish.entity.vo.request.DoctorPrescriptionItemSubmitVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionPreviewItemVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionPreviewVO;
import cn.gugufish.entity.vo.response.ConsultationPrescriptionVO;
import cn.gugufish.mapper.ConsultationPrescriptionMapper;
import cn.gugufish.mapper.MedicineCatalogMapper;
import cn.gugufish.mapper.MedicineInteractionMapper;
import cn.gugufish.mapper.MedicineWarningMapper;
import cn.gugufish.service.ConsultationPrescriptionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConsultationPrescriptionServiceImpl extends ServiceImpl<ConsultationPrescriptionMapper, ConsultationPrescription>
        implements ConsultationPrescriptionService {

    @Resource
    MedicineCatalogMapper medicineCatalogMapper;

    @Resource
    MedicineWarningMapper medicineWarningMapper;

    @Resource
    MedicineInteractionMapper medicineInteractionMapper;

    @Override
    public List<ConsultationPrescriptionVO> listByConsultationId(int consultationId) {
        return this.list(Wrappers.<ConsultationPrescription>query()
                        .eq("consultation_id", consultationId)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationPrescriptionVO.class))
                .toList();
    }

    @Override
    public ConsultationPrescriptionPreviewVO preview(List<DoctorPrescriptionItemSubmitVO> items) {
        return buildPreview(items, false).preview;
    }

    @Override
    @Transactional
    public String replaceConsultationPrescriptions(int consultationId, Doctor doctor, List<DoctorPrescriptionItemSubmitVO> items, Date now) {
        if (items == null) return null;
        if (doctor == null) return "当前医生信息无效，无法保存处方";

        PrescriptionBuildResult result = buildPreview(items, true);
        if (result.errorMessage != null) return result.errorMessage;
        if (result.preview != null && Objects.equals(result.preview.getConflictDetected(), 1)) {
            return "当前处方存在不可同时用药，请调整后再提交";
        }

        this.remove(Wrappers.<ConsultationPrescription>query().eq("consultation_id", consultationId));
        if (result.normalizedItems.isEmpty()) return null;

        List<ConsultationPrescription> entities = new ArrayList<>();
        for (int index = 0; index < result.normalizedItems.size(); index++) {
            PrescriptionNormalizedItem item = result.normalizedItems.get(index);
            MedicineCatalog medicine = item.medicine;
            entities.add(new ConsultationPrescription(
                    null,
                    consultationId,
                    doctor.getId(),
                    medicine.getId(),
                    medicine.getName(),
                    medicine.getGenericName(),
                    medicine.getCategoryName(),
                    medicine.getSpecification(),
                    item.dosage,
                    item.frequency,
                    item.durationDays,
                    item.medicationInstruction,
                    item.warningSummary,
                    index,
                    now,
                    now
            ));
        }
        return this.saveBatch(entities) ? null : "处方保存失败，请稍后重试";
    }

    private PrescriptionBuildResult buildPreview(List<DoctorPrescriptionItemSubmitVO> items, boolean strict) {
        List<DoctorPrescriptionItemSubmitVO> source = items == null ? List.of() : items;
        List<DoctorPrescriptionItemSubmitVO> effectiveItems = new ArrayList<>();
        for (DoctorPrescriptionItemSubmitVO item : source) {
            if (item == null) continue;
            if (item.getMedicineId() == null
                    && trimToNull(item.getDosage()) == null
                    && trimToNull(item.getFrequency()) == null
                    && item.getDurationDays() == null
                    && trimToNull(item.getMedicationInstruction()) == null) {
                continue;
            }
            effectiveItems.add(item);
        }

        PrescriptionBuildResult result = new PrescriptionBuildResult();
        if (effectiveItems.isEmpty()) {
            result.preview = emptyPreview();
            result.normalizedItems = List.of();
            return result;
        }

        Map<Integer, Integer> duplicateCounter = new HashMap<>();
        LinkedHashSet<Integer> medicineIdSet = new LinkedHashSet<>();
        for (DoctorPrescriptionItemSubmitVO item : effectiveItems) {
            if (item.getMedicineId() == null || item.getMedicineId() <= 0) continue;
            medicineIdSet.add(item.getMedicineId());
            duplicateCounter.put(item.getMedicineId(), duplicateCounter.getOrDefault(item.getMedicineId(), 0) + 1);
        }

        Map<Integer, MedicineCatalog> medicineMap = new HashMap<>();
        if (!medicineIdSet.isEmpty()) {
            medicineCatalogMapper.selectBatchIds(new ArrayList<>(medicineIdSet))
                    .forEach(item -> medicineMap.put(item.getId(), item));
        }
        Map<Integer, List<String>> warningMap = loadWarningMap(new ArrayList<>(medicineIdSet));
        Map<String, String> interactionWarningMap = loadInteractionWarningMap(new ArrayList<>(medicineIdSet));

        List<String> validationWarnings = new ArrayList<>();
        List<PrescriptionNormalizedItem> normalizedItems = new ArrayList<>();
        for (int index = 0; index < effectiveItems.size(); index++) {
            DoctorPrescriptionItemSubmitVO item = effectiveItems.get(index);
            int rowNumber = index + 1;
            Integer medicineId = item.getMedicineId();
            if (medicineId == null || medicineId <= 0) {
                if (strict) {
                    result.errorMessage = "请先选择第 " + rowNumber + " 个药品";
                    return result;
                }
                validationWarnings.add("第 " + rowNumber + " 条处方尚未选择药品");
                continue;
            }

            MedicineCatalog medicine = medicineMap.get(medicineId);
            if (medicine == null || medicine.getStatus() == null || medicine.getStatus() != 1) {
                result.errorMessage = "第 " + rowNumber + " 个药品不存在或已停用";
                return result;
            }

            if (duplicateCounter.getOrDefault(medicineId, 0) > 1) {
                String warning = "药品“" + medicine.getName() + "”重复选择，请合并为一条处方";
                validationWarnings.add(warning);
                if (strict) {
                    result.errorMessage = warning;
                    return result;
                }
            }

            String dosage = trimToNull(item.getDosage());
            String frequency = trimToNull(item.getFrequency());
            Integer durationDays = item.getDurationDays();
            String medicationInstruction = trimToNull(item.getMedicationInstruction());
            if (strict) {
                if (dosage == null) {
                    result.errorMessage = "请填写第 " + rowNumber + " 个药品的单次剂量";
                    return result;
                }
                if (frequency == null) {
                    result.errorMessage = "请填写第 " + rowNumber + " 个药品的用药频次";
                    return result;
                }
                if (durationDays == null || durationDays <= 0) {
                    result.errorMessage = "请填写第 " + rowNumber + " 个药品的用药天数";
                    return result;
                }
            }

            PrescriptionNormalizedItem normalized = new PrescriptionNormalizedItem();
            normalized.medicine = medicine;
            normalized.dosage = dosage;
            normalized.frequency = frequency;
            normalized.durationDays = durationDays;
            normalized.medicationInstruction = medicationInstruction;
            normalized.warningDetails = new ArrayList<>(warningMap.getOrDefault(medicineId, List.of()));
            normalizedItems.add(normalized);
        }

        LinkedHashSet<String> overallWarnings = new LinkedHashSet<>();
        LinkedHashSet<String> conflictWarnings = new LinkedHashSet<>();
        for (PrescriptionNormalizedItem item : normalizedItems) {
            overallWarnings.addAll(item.warningDetails);
        }

        for (int leftIndex = 0; leftIndex < normalizedItems.size(); leftIndex++) {
            PrescriptionNormalizedItem left = normalizedItems.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < normalizedItems.size(); rightIndex++) {
                PrescriptionNormalizedItem right = normalizedItems.get(rightIndex);
                String interactionWarning = interactionWarningMap.get(interactionKey(left.medicine.getId(), right.medicine.getId()));
                if (interactionWarning == null) continue;
                left.warningDetails.add("联用禁忌：" + interactionWarning);
                right.warningDetails.add("联用禁忌：" + interactionWarning);
                conflictWarnings.add(interactionWarning);
                overallWarnings.add(interactionWarning);
            }
        }

        List<ConsultationPrescriptionPreviewItemVO> previewItems = new ArrayList<>();
        for (PrescriptionNormalizedItem item : normalizedItems) {
            item.warningDetails = item.warningDetails.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            item.warningSummary = item.warningDetails.isEmpty() ? null : String.join("；", item.warningDetails);

            ConsultationPrescriptionPreviewItemVO previewItem = new ConsultationPrescriptionPreviewItemVO();
            previewItem.setMedicineId(item.medicine.getId());
            previewItem.setMedicineName(item.medicine.getName());
            previewItem.setSpecification(item.medicine.getSpecification());
            previewItem.setWarningSummary(item.warningSummary);
            previewItem.setWarningDetails(item.warningDetails);
            previewItems.add(previewItem);
        }

        ConsultationPrescriptionPreviewVO preview = new ConsultationPrescriptionPreviewVO();
        preview.setConflictDetected(conflictWarnings.isEmpty() ? 0 : 1);
        preview.setOverallWarnings(new ArrayList<>(overallWarnings));
        preview.setConflictWarnings(new ArrayList<>(conflictWarnings));
        preview.setValidationWarnings(validationWarnings.stream().distinct().toList());
        preview.setItems(previewItems);

        result.preview = preview;
        result.normalizedItems = normalizedItems;
        return result;
    }

    private Map<Integer, List<String>> loadWarningMap(List<Integer> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) return Map.of();
        Map<Integer, List<String>> warningMap = new HashMap<>();
        medicineWarningMapper.selectList(Wrappers.<MedicineWarning>query()
                        .in("medicine_id", medicineIds)
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .forEach(item -> warningMap.computeIfAbsent(item.getMedicineId(), key -> new ArrayList<>()).add(item.getWarningText()));
        return warningMap;
    }

    private Map<String, String> loadInteractionWarningMap(List<Integer> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) return Map.of();
        Map<String, String> interactionMap = new HashMap<>();
        medicineInteractionMapper.selectList(Wrappers.<MedicineInteraction>query()
                        .eq("status", 1)
                        .and(wrapper -> wrapper.in("medicine_id", medicineIds).or().in("conflict_medicine_id", medicineIds)))
                .forEach(item -> interactionMap.put(
                        interactionKey(item.getMedicineId(), item.getConflictMedicineId()),
                        buildInteractionWarning(item)
                ));
        return interactionMap;
    }

    private String buildInteractionWarning(MedicineInteraction interaction) {
        String customText = trimToNull(interaction == null ? null : interaction.getInteractionText());
        if (customText != null) return customText;
        Integer leftId = interaction == null ? null : interaction.getMedicineId();
        Integer rightId = interaction == null ? null : interaction.getConflictMedicineId();
        MedicineCatalog left = leftId == null ? null : medicineCatalogMapper.selectById(leftId);
        MedicineCatalog right = rightId == null ? null : medicineCatalogMapper.selectById(rightId);
        String leftName = left == null ? "当前药品" : left.getName();
        String rightName = right == null ? "目标药品" : right.getName();
        return "药品“" + leftName + "”与“" + rightName + "”不建议同时使用，请调整处方";
    }

    private String interactionKey(Integer leftId, Integer rightId) {
        int left = leftId == null ? 0 : leftId;
        int right = rightId == null ? 0 : rightId;
        return left < right ? left + "_" + right : right + "_" + left;
    }

    private ConsultationPrescriptionPreviewVO emptyPreview() {
        ConsultationPrescriptionPreviewVO preview = new ConsultationPrescriptionPreviewVO();
        preview.setConflictDetected(0);
        preview.setOverallWarnings(List.of());
        preview.setConflictWarnings(List.of());
        preview.setValidationWarnings(List.of());
        preview.setItems(List.of());
        return preview;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private static final class PrescriptionBuildResult {
        String errorMessage;
        ConsultationPrescriptionPreviewVO preview;
        List<PrescriptionNormalizedItem> normalizedItems = List.of();
    }

    private static final class PrescriptionNormalizedItem {
        MedicineCatalog medicine;
        String dosage;
        String frequency;
        Integer durationDays;
        String medicationInstruction;
        List<String> warningDetails;
        String warningSummary;
    }
}
