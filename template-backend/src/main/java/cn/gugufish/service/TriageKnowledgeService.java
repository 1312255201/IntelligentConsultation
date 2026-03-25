package cn.gugufish.service;

import cn.gugufish.entity.dto.TriageKnowledge;
import cn.gugufish.entity.vo.request.TriageKnowledgeCreateVO;
import cn.gugufish.entity.vo.request.TriageKnowledgeUpdateVO;

import java.util.List;

public interface TriageKnowledgeService {
    List<TriageKnowledge> listKnowledgeItems();
    String createKnowledge(TriageKnowledgeCreateVO vo);
    String updateKnowledge(TriageKnowledgeUpdateVO vo);
    String deleteKnowledge(int id);
}
