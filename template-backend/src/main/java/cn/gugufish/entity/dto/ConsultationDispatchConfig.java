package cn.gugufish.entity.dto;

import cn.gugufish.entity.BaseData;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_consultation_dispatch_config")
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDispatchConfig implements BaseData {
    @TableId
    Integer id;
    Integer visitTypeWeight;
    Integer scheduleWeight;
    Integer capacityWeight;
    Integer workloadWeight;
    Integer tagMatchWeight;
    Integer tagMatchScorePerHit;
    Integer maxMatchedTags;
    Integer recommendDoctorLimit;
    Integer minRecommendationScore;
    Integer maxRecommendationScore;
    Integer waitingOverdueHours;
    Date updateTime;
}
