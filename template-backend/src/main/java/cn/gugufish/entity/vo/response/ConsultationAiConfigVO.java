package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationAiConfigVO {
    Integer id;
    Integer enabled;
    String promptVersion;
    Integer doctorCandidateLimit;
    Date updateTime;
}
