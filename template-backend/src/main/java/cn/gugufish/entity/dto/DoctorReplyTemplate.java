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
@TableName("db_doctor_reply_template")
@NoArgsConstructor
@AllArgsConstructor
public class DoctorReplyTemplate implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer doctorId;
    String sceneType;
    String title;
    String content;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
