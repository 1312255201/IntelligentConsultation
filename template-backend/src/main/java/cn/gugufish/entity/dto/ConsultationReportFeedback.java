package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_consultation_report_feedback")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationReportFeedback implements BaseData {
    @TableId(type = IdType.AUTO)
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
}
