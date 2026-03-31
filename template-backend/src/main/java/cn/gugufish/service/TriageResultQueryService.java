package cn.gugufish.service;

import cn.gugufish.entity.vo.response.TriageResultVO;

public interface TriageResultQueryService {
    TriageResultVO detailByConsultationId(int consultationId);
}
