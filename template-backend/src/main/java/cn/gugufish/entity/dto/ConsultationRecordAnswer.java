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
@TableName("db_consultation_record_answer")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRecordAnswer implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    String fieldCode;
    String fieldLabel;
    String fieldType;
    String fieldValue;
    Integer sort;
    Date createTime;
    Date updateTime;
}
