package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationPrescription;
import cn.gugufish.entity.dto.MedicineCatalog;
import cn.gugufish.entity.dto.MedicineInteraction;
import cn.gugufish.entity.dto.MedicineWarning;
import cn.gugufish.entity.vo.request.MedicineCreateVO;
import cn.gugufish.entity.vo.request.MedicineUpdateVO;
import cn.gugufish.entity.vo.response.MedicineCatalogVO;
import cn.gugufish.mapper.ConsultationPrescriptionMapper;
import cn.gugufish.mapper.MedicineCatalogMapper;
import cn.gugufish.mapper.MedicineInteractionMapper;
import cn.gugufish.mapper.MedicineWarningMapper;
import cn.gugufish.service.MedicineCatalogService;
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

@Service
public class MedicineCatalogServiceImpl extends ServiceImpl<MedicineCatalogMapper, MedicineCatalog> implements MedicineCatalogService {

    @Resource
    ConsultationPrescriptionMapper consultationPrescriptionMapper;

    @Resource
    MedicineWarningMapper medicineWarningMapper;

    @Resource
    MedicineInteractionMapper medicineInteractionMapper;

    @Override
    public List<MedicineCatalogVO> listMedicines(boolean enabledOnly) {
        List<MedicineCatalog> medicines = this.list(Wrappers.<MedicineCatalog>query()
                .eq(enabledOnly, "status", 1)
                .orderByAsc("sort")
                .orderByAsc("id"));
        if (medicines.isEmpty()) return List.of();

        List<Integer> medicineIds = medicines.stream().map(MedicineCatalog::getId).toList();
        Map<Integer, List<String>> warningMap = loadWarningMap(medicineIds);
        Map<Integer, List<Integer>> interactionMap = loadInteractionMap(medicineIds);
        return medicines.stream()
                .map(item -> toViewObject(item, warningMap, interactionMap))
                .toList();
    }

    @Override
    @Transactional
    public String createMedicine(MedicineCreateVO vo) {
        String message = validateMedicine(null, vo);
        if (message != null) return message;

        Date now = new Date();
        MedicineCatalog medicine = new MedicineCatalog(
                null,
                vo.getName().trim(),
                trimToNull(vo.getGenericName()),
                trimToNull(vo.getCategoryName()),
                trimToNull(vo.getSpecification()),
                vo.getSort() == null ? 0 : vo.getSort(),
                vo.getStatus() == null ? 1 : (vo.getStatus() == 1 ? 1 : 0),
                now,
                now
        );
        if (!this.save(medicine)) {
            return "药品新增失败，请稍后重试";
        }
        replaceWarnings(medicine.getId(), vo.getWarningTexts(), now);
        replaceInteractions(medicine.getId(), vo.getConflictMedicineIds(), now);
        return null;
    }

    @Override
    @Transactional
    public String updateMedicine(MedicineUpdateVO vo) {
        MedicineCatalog current = this.getById(vo.getId());
        if (current == null) return "药品不存在";

        String message = validateMedicine(vo.getId(), vo);
        if (message != null) return message;

        Date now = new Date();
        boolean updated = this.update(Wrappers.<MedicineCatalog>update()
                .eq("id", vo.getId())
                .set("name", vo.getName().trim())
                .set("generic_name", trimToNull(vo.getGenericName()))
                .set("category_name", trimToNull(vo.getCategoryName()))
                .set("specification", trimToNull(vo.getSpecification()))
                .set("sort", vo.getSort() == null ? 0 : vo.getSort())
                .set("status", vo.getStatus() == null ? 1 : (vo.getStatus() == 1 ? 1 : 0))
                .set("update_time", now));
        if (!updated) {
            return "药品更新失败，请稍后重试";
        }
        replaceWarnings(vo.getId(), vo.getWarningTexts(), now);
        replaceInteractions(vo.getId(), vo.getConflictMedicineIds(), now);
        return null;
    }

    @Override
    @Transactional
    public String deleteMedicine(int id) {
        MedicineCatalog current = this.getById(id);
        if (current == null) return "药品不存在";
        if (consultationPrescriptionMapper.exists(Wrappers.<ConsultationPrescription>query().eq("medicine_id", id))) {
            return "该药品已被历史处方引用，请先停用，不建议直接删除";
        }
        medicineWarningMapper.delete(Wrappers.<MedicineWarning>query().eq("medicine_id", id));
        medicineInteractionMapper.delete(Wrappers.<MedicineInteraction>query()
                .eq("medicine_id", id)
                .or()
                .eq("conflict_medicine_id", id));
        return this.removeById(id) ? null : "药品删除失败，请稍后重试";
    }

    private String validateMedicine(Integer id, MedicineCreateVO vo) {
        if (vo == null) return "药品参数不能为空";
        String name = trimToNull(vo.getName());
        if (name == null) return "请填写药品名称";

        String specification = trimToNull(vo.getSpecification());
        if (this.baseMapper.exists(Wrappers.<MedicineCatalog>query()
                .eq("name", name)
                .eq(specification != null, "specification", specification)
                .isNull(specification == null, "specification")
                .ne(id != null, "id", id))) {
            return "同名同规格药品已存在";
        }

        List<String> warningTexts = normalizeWarningTexts(vo.getWarningTexts());
        if (warningTexts.size() > 12) return "单个药品最多维护 12 条禁忌提醒";

        List<Integer> conflictMedicineIds = normalizeConflictMedicineIds(vo.getConflictMedicineIds(), id);
        if (!conflictMedicineIds.isEmpty()) {
            long validCount = this.count(Wrappers.<MedicineCatalog>query().in("id", conflictMedicineIds));
            if (validCount != conflictMedicineIds.size()) return "存在无效的联用冲突药品";
        }
        return null;
    }

    private void replaceWarnings(int medicineId, List<String> warningTexts, Date now) {
        medicineWarningMapper.delete(Wrappers.<MedicineWarning>query().eq("medicine_id", medicineId));
        List<String> normalized = normalizeWarningTexts(warningTexts);
        for (int index = 0; index < normalized.size(); index++) {
            medicineWarningMapper.insert(new MedicineWarning(
                    null,
                    medicineId,
                    normalized.get(index),
                    index,
                    1,
                    now,
                    now
            ));
        }
    }

    private void replaceInteractions(int medicineId, List<Integer> conflictMedicineIds, Date now) {
        medicineInteractionMapper.delete(Wrappers.<MedicineInteraction>query().eq("medicine_id", medicineId));
        List<Integer> normalized = normalizeConflictMedicineIds(conflictMedicineIds, medicineId);
        for (Integer conflictMedicineId : normalized) {
            medicineInteractionMapper.insert(new MedicineInteraction(
                    null,
                    medicineId,
                    conflictMedicineId,
                    null,
                    1,
                    now,
                    now
            ));
        }
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

    private Map<Integer, List<Integer>> loadInteractionMap(List<Integer> medicineIds) {
        if (medicineIds == null || medicineIds.isEmpty()) return Map.of();
        Map<Integer, List<Integer>> interactionMap = new HashMap<>();
        medicineInteractionMapper.selectList(Wrappers.<MedicineInteraction>query()
                        .in("medicine_id", medicineIds)
                        .eq("status", 1)
                        .orderByAsc("id"))
                .forEach(item -> interactionMap.computeIfAbsent(item.getMedicineId(), key -> new ArrayList<>()).add(item.getConflictMedicineId()));
        return interactionMap;
    }

    private MedicineCatalogVO toViewObject(MedicineCatalog medicine,
                                           Map<Integer, List<String>> warningMap,
                                           Map<Integer, List<Integer>> interactionMap) {
        if (medicine == null) return null;
        return medicine.asViewObject(MedicineCatalogVO.class, vo -> {
            vo.setWarningTexts(warningMap.getOrDefault(medicine.getId(), List.of()));
            vo.setConflictMedicineIds(interactionMap.getOrDefault(medicine.getId(), List.of()));
        });
    }

    private List<String> normalizeWarningTexts(List<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String text = trimToNull(value);
                if (text == null) continue;
                if (text.length() > 255) {
                    text = text.substring(0, 255);
                }
                set.add(text);
            }
        }
        return new ArrayList<>(set);
    }

    private List<Integer> normalizeConflictMedicineIds(List<Integer> values, Integer currentId) {
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        if (values != null) {
            for (Integer value : values) {
                if (value == null || value <= 0) continue;
                if (currentId != null && currentId.equals(value)) continue;
                set.add(value);
            }
        }
        return new ArrayList<>(set);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
