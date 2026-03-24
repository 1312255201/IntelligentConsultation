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
@TableName("db_doctor_service_tag")
@NoArgsConstructor
@AllArgsConstructor
public class DoctorServiceTag implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer doctorId;
    String tagCode;
    String tagName;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
