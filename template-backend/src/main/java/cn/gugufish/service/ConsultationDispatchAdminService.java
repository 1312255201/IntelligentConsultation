package cn.gugufish.service;

import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.vo.response.ConsultationDispatchBatchCompareVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchPreviewRecordVO;
import cn.gugufish.entity.vo.response.ConsultationDispatchPreviewVO;

import java.util.List;

public interface ConsultationDispatchAdminService {
    List<ConsultationDispatchPreviewRecordVO> listPreviewRecords(int limit);
    ConsultationDispatchPreviewVO preview(int consultationId, ConsultationDispatchConfig config);
    ConsultationDispatchBatchCompareVO batchPreview(List<Integer> consultationIds, ConsultationDispatchConfig currentConfig);
}
