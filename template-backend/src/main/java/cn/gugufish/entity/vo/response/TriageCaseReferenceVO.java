package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TriageCaseReferenceVO {
    Integer id;
    String title;
    String chiefComplaint;
    String symptomSummary;
    String triageResult;
    Integer departmentId;
    Integer doctorId;
    String riskLevel;
    String tags;
    String sourceType;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
