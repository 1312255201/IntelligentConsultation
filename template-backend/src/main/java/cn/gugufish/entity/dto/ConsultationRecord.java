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
@TableName("db_consultation_record")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRecord implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String consultationNo;
    Integer accountId;
    Integer patientId;
    String patientName;
    Integer categoryId;
    String categoryName;
    Integer departmentId;
    String departmentName;
    Integer templateId;
    String templateName;
    String title;
    String chiefComplaint;
    String healthSummary;
    String status;
    Integer answerCount;
    Integer triageLevelId;
    String triageLevelCode;
    String triageLevelName;
    String triageLevelColor;
    String triageActionType;
    String triageSuggestion;
    String triageRuleSummary;
    Date createTime;
    Date updateTime;
}
