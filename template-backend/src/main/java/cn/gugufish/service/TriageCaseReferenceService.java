package cn.gugufish.service;

import cn.gugufish.entity.dto.TriageCaseReference;
import cn.gugufish.entity.vo.request.TriageCaseReferenceCreateVO;
import cn.gugufish.entity.vo.request.TriageCaseReferenceUpdateVO;

import java.util.List;

public interface TriageCaseReferenceService {
    List<TriageCaseReference> listCaseReferences();
    String createCaseReference(TriageCaseReferenceCreateVO vo);
    String updateCaseReference(TriageCaseReferenceUpdateVO vo);
    String deleteCaseReference(int id);
}
