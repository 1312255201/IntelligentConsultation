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
@TableName("db_doctor_schedule")
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSchedule implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer doctorId;
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
