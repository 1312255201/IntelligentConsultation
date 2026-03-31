package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class DoctorWorkbenchVO {
    Integer bound;
    String bindingMessage;
    Integer doctorId;
    Integer doctorStatus;
    Integer accountId;
    Integer departmentId;
    String departmentName;
    String doctorName;
    String doctorTitle;
    String photo;
    String introduction;
    String expertise;
    Integer consultationCount;
    Integer todayConsultationCount;
    Integer riskConsultationCount;
    Integer upcomingScheduleCount;
    Integer serviceTagCount;
    List<String> serviceTags;
    List<AdminConsultationRecordVO> recentConsultations;
    List<DoctorScheduleVO> upcomingSchedules;
}
