package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ConsultationDispatchPreviewVO {
    Integer id;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String title;
    String chiefComplaint;
    String triageLevelName;
    String triageActionType;
    String triageSuggestion;
    String triageRuleSummary;
    Date createTime;
    List<ConsultationRecommendDoctorVO> recommendedDoctors;
}
