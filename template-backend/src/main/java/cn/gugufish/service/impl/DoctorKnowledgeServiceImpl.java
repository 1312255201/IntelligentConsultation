package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.TriageKnowledge;
import cn.gugufish.entity.vo.request.TriageKnowledgeCreateVO;
import cn.gugufish.entity.vo.request.TriageKnowledgeUpdateVO;
import cn.gugufish.entity.vo.response.TriageKnowledgeVO;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.TriageKnowledgeMapper;
import cn.gugufish.service.DoctorKnowledgeService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DoctorKnowledgeServiceImpl implements DoctorKnowledgeService {

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    TriageKnowledgeMapper triageKnowledgeMapper;

    @Override
    public List<TriageKnowledgeVO> listKnowledgeItems(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return List.of();
        return triageKnowledgeMapper.selectList(Wrappers.<TriageKnowledge>query()
                        .eq("doctor_id", doctor.getId())
                        .orderByAsc("sort")
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .stream()
                .map(item -> item.asViewObject(TriageKnowledgeVO.class))
                .toList();
    }

    @Override
    public String createKnowledge(int accountId, TriageKnowledgeCreateVO vo) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null || doctor.getDepartmentId() == null) return "当前医生账号尚未绑定医生档案";
        if (existsByTypeAndTitle(vo.getKnowledgeType(), vo.getTitle(), null)) {
            return "同类型下的知识标题已存在";
        }

        Date now = new Date();
        TriageKnowledge knowledge = new TriageKnowledge(
                null,
                vo.getKnowledgeType().trim(),
                vo.getTitle().trim(),
                vo.getContent().trim(),
                blankToNull(vo.getTags()),
                doctor.getDepartmentId(),
                doctor.getId(),
                resolveSourceType(vo.getSourceType()),
                defaultVersion(vo.getVersion()),
                defaultSort(vo.getSort()),
                defaultStatus(vo.getStatus()),
                now,
                now
        );
        return triageKnowledgeMapper.insert(knowledge) > 0 ? null : "医生知识新增失败，请稍后重试";
    }

    @Override
    public String updateKnowledge(int accountId, TriageKnowledgeUpdateVO vo) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null || doctor.getDepartmentId() == null) return "当前医生账号尚未绑定医生档案";

        TriageKnowledge current = triageKnowledgeMapper.selectOne(Wrappers.<TriageKnowledge>query()
                .eq("id", vo.getId())
                .eq("doctor_id", doctor.getId())
                .last("limit 1"));
        if (current == null) return "当前知识记录不存在或无权修改";
        if (existsByTypeAndTitle(vo.getKnowledgeType(), vo.getTitle(), vo.getId())) {
            return "同类型下的知识标题已存在";
        }

        current.setKnowledgeType(vo.getKnowledgeType().trim());
        current.setTitle(vo.getTitle().trim());
        current.setContent(vo.getContent().trim());
        current.setTags(blankToNull(vo.getTags()));
        current.setDepartmentId(doctor.getDepartmentId());
        current.setDoctorId(doctor.getId());
        current.setSourceType(resolveSourceType(vo.getSourceType()));
        current.setVersion(defaultVersion(vo.getVersion()));
        current.setSort(defaultSort(vo.getSort()));
        current.setStatus(defaultStatus(vo.getStatus()));
        current.setUpdateTime(new Date());
        return triageKnowledgeMapper.updateById(current) > 0 ? null : "医生知识更新失败，请稍后重试";
    }

    @Override
    public String deleteKnowledge(int accountId, int id) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return "当前医生账号尚未绑定医生档案";
        TriageKnowledge current = triageKnowledgeMapper.selectOne(Wrappers.<TriageKnowledge>query()
                .eq("id", id)
                .eq("doctor_id", doctor.getId())
                .last("limit 1"));
        if (current == null) return "当前知识记录不存在或无权删除";
        return triageKnowledgeMapper.deleteById(id) > 0 ? null : "医生知识删除失败，请稍后重试";
    }

    private Doctor currentDoctor(int accountId) {
        return doctorMapper.selectOne(Wrappers.<Doctor>query()
                .eq("account_id", accountId)
                .last("limit 1"));
    }

    private boolean existsByTypeAndTitle(String knowledgeType, String title, Integer ignoreId) {
        return triageKnowledgeMapper.exists(Wrappers.<TriageKnowledge>query()
                .eq("knowledge_type", knowledgeType.trim())
                .eq("title", title.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }

    private Integer defaultVersion(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private Integer defaultSort(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private Integer defaultStatus(Integer value) {
        return value == null || value < 0 ? 1 : value;
    }

    private String resolveSourceType(String value) {
        String text = blankToNull(value);
        return text == null ? "doctor_manual" : text;
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
