package cn.gugufish.service;

import cn.gugufish.entity.vo.response.TriageSessionVO;

public interface TriageSessionQueryService {
    TriageSessionVO detailByConsultationId(int consultationId);
}
