package cn.gugufish.service;

import cn.gugufish.entity.vo.request.TriageKnowledgeCreateVO;
import cn.gugufish.entity.vo.request.TriageKnowledgeUpdateVO;
import cn.gugufish.entity.vo.response.TriageKnowledgeVO;

import java.util.List;

public interface DoctorKnowledgeService {
    List<TriageKnowledgeVO> listKnowledgeItems(int accountId);
    String createKnowledge(int accountId, TriageKnowledgeCreateVO vo);
    String updateKnowledge(int accountId, TriageKnowledgeUpdateVO vo);
    String deleteKnowledge(int accountId, int id);
}
