package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationArchiveSummaryVO {
    String stageText;
    String overview;
    String triageSummary;
    String doctorSummary;
    String archiveConclusion;
    String followUpSummary;
    String serviceSummary;
    String latestMessageSummary;
    String patientActionHint;
    String doctorName;
    Date lastUpdateTime;
    Integer messageCount;
    Integer followUpCount;
    Integer serviceScore;
    List<String> riskFlags;
    List<String> conclusionTags;
    List<String> nextActions;
    String plainText;
}
