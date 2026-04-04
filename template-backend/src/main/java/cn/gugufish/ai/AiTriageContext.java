package cn.gugufish.ai;

import cn.gugufish.entity.dto.ConsultationRecord;
import cn.gugufish.entity.dto.ConsultationRecordAnswer;
import cn.gugufish.entity.dto.TriageMessage;
import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiTriageContext {
    ConsultationRecord record;
    List<ConsultationRecordAnswer> answers;
    List<ConsultationRecommendDoctorVO> doctorCandidates;
    List<TriageMessage> triageMessages;
    String userMessage;
}
