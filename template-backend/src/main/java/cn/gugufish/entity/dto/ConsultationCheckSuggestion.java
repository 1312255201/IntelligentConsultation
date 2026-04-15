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
@TableName("db_consultation_check_suggestion")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationCheckSuggestion implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer consultationId;
    Integer doctorId;
    String doctorName;
    Integer departmentId;
    String departmentName;
    String itemName;
    String itemType;
    String urgencyLevel;
    String purpose;
    String attentionNote;
    Integer status;
    Integer sort;
    Date createTime;
    Date updateTime;
}
