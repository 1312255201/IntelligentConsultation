package cn.gugufish.service;

import cn.gugufish.entity.vo.request.ConsultationIntakeRouteRequestVO;
import cn.gugufish.entity.vo.response.ConsultationIntakeRouteVO;

public interface ConsultationIntakeRoutingService {
    ConsultationIntakeRouteVO route(int accountId, ConsultationIntakeRouteRequestVO vo);
}
