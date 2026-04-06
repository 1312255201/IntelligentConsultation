package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationDispatchPreviewRecordVO {
    Integer id;
    String consultationNo;
    String patientName;
    String categoryName;
    String departmentName;
    String title;
    String chiefComplaint;
    String triageLevelName;
    String triageActionType;
    Date createTime;
}
