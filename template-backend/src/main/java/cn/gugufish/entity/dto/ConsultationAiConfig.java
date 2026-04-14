package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_consultation_ai_config")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationAiConfig implements BaseData {
    @TableId
    Integer id;
    Integer enabled;
    String promptVersion;
    Integer doctorCandidateLimit;
    Date updateTime;
}
