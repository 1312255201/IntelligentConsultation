package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationReportFeedbackVO {
    Integer id;
    Integer consultationId;
    Integer suggestionId;
    Integer accountId;
    Integer patientId;
    String patientName;
    String reportType;
    String reportName;
    String reportSummary;
    Date reportDate;
    String doctorQuestion;
    String attachmentsJson;
    Integer status;
    Date createTime;
    Date updateTime;
    List<String> attachments;
}
