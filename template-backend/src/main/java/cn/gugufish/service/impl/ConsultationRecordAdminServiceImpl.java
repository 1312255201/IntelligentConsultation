package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDoctorAssignment;
import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.TriageRuleHitLog;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.entity.vo.response.ConsultationRecordAnswerVO;
import cn.gugufish.entity.vo.response.TriageRuleHitLogVO;
import cn.gugufish.mapper.ConsultationDoctorAssignmentMapper;
import cn.gugufish.mapper.ConsultationRecordAnswerMapper;
import cn.gugufish.mapper.ConsultationRecordMapper;
import cn.gugufish.mapper.TriageRuleHitLogMapper;
import cn.gugufish.service.ConsultationDoctorAssignmentQueryService;
import cn.gugufish.service.ConsultationRecordAdminService;
import cn.gugufish.service.ConsultationDoctorConclusionQueryService;
import cn.gugufish.service.ConsultationDoctorFollowUpQueryService;
import cn.gugufish.service.ConsultationDoctorHandleQueryService;
import cn.gugufish.service.TriageFeedbackQueryService;
import cn.gugufish.service.TriageResultQueryService;
import cn.gugufish.service.TriageSessionQueryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsultationRecordAdminServiceImpl implements ConsultationRecordAdminService {

    @Resource
    ConsultationRecordMapper consultationRecordMapper;

    @Resource
    ConsultationRecordAnswerMapper consultationRecordAnswerMapper;

    @Resource
    ConsultationDoctorAssignmentMapper consultationDoctorAssignmentMapper;

    @Resource
    TriageRuleHitLogMapper triageRuleHitLogMapper;

    @Resource
    TriageSessionQueryService triageSessionQueryService;

    @Resource
    TriageResultQueryService triageResultQueryService;

    @Resource
    TriageFeedbackQueryService triageFeedbackQueryService;

    @Resource
    ConsultationDoctorAssignmentQueryService consultationDoctorAssignmentQueryService;

    @Resource
    ConsultationDoctorHandleQueryService consultationDoctorHandleQueryService;

    @Resource
    ConsultationDoctorConclusionQueryService consultationDoctorConclusionQueryService;

    @Resource
    ConsultationDoctorFollowUpQueryService consultationDoctorFollowUpQueryService;

    @Override
    public List<AdminConsultationRecordVO> listRecords() {
        List<ConsultationRecord> records = consultationRecordMapper.selectList(Wrappers.<ConsultationRecord>query()
                .orderByDesc("create_time")
                .orderByDesc("id"));
        if (records.isEmpty()) return List.of();

        List<Integer> consultationIds = records.stream().map(ConsultationRecord::getId).toList();
        Map<Integer, ConsultationDoctorAssignment> assignmentMap = new HashMap<>();
        consultationDoctorAssignmentMapper.selectList(Wrappers.<ConsultationDoctorAssignment>query()
                        .in("consultation_id", consultationIds))
                .stream()
                .forEach(item -> assignmentMap.put(item.getConsultationId(), item));

        return records.stream()
                .map(item -> item.asViewObject(AdminConsultationRecordVO.class, vo -> {
                    ConsultationDoctorAssignment assignment = assignmentMap.get(item.getId());
                    if (assignment != null) {
                        vo.setDoctorAssignment(assignment.asViewObject(cn.gugufish.entity.vo.response.ConsultationDoctorAssignmentVO.class));
                    }
                }))
                .toList();
    }

    @Override
    public AdminConsultationRecordVO recordDetail(int id) {
        ConsultationRecord record = consultationRecordMapper.selectById(id);
        if (record == null) return null;

        List<ConsultationRecordAnswerVO> answers = consultationRecordAnswerMapper.selectList(Wrappers.<ConsultationRecordAnswer>query()
                        .eq("consultation_id", id)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(ConsultationRecordAnswerVO.class))
                .toList();

        List<TriageRuleHitLogVO> ruleHits = triageRuleHitLogMapper.selectList(Wrappers.<TriageRuleHitLog>query()
                        .eq("consultation_id", id)
                        .orderByDesc("is_primary")
                        .orderByDesc("priority")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(TriageRuleHitLogVO.class))
                .toList();

        return record.asViewObject(AdminConsultationRecordVO.class, vo -> {
            vo.setAnswers(answers);
            vo.setRuleHits(ruleHits);
            vo.setDoctorAssignment(consultationDoctorAssignmentQueryService.detailByConsultationId(id));
            vo.setDoctorHandle(consultationDoctorHandleQueryService.detailByConsultationId(id));
            vo.setDoctorConclusion(consultationDoctorConclusionQueryService.detailByConsultationId(id));
            vo.setDoctorFollowUps(consultationDoctorFollowUpQueryService.listByConsultationId(id));
            vo.setTriageSession(triageSessionQueryService.detailByConsultationId(id));
            vo.setTriageResult(triageResultQueryService.detailByConsultationId(id));
            vo.setTriageFeedback(triageFeedbackQueryService.detailByConsultationId(id));
        });
    }
}
