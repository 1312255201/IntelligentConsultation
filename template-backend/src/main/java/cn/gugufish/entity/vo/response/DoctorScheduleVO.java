package cn.gugufish.entity.vo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class DoctorScheduleVO {
    Integer id;
    Integer doctorId;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    Date scheduleDate;
    String timePeriod;
    String visitType;
    Integer maxCapacity;
    Integer usedCapacity;
    Integer status;
    String remark;
    Date createTime;
    Date updateTime;
}
