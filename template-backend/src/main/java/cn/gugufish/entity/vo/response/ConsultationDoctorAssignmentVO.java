package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ConsultationDoctorAssignmentVO {
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String status;
    Date claimTime;
    Date releaseTime;
    Date createTime;
    Date updateTime;
}
