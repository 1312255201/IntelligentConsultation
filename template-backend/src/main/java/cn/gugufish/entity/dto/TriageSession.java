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
@TableName("db_triage_session")
@NoArgsConstructor
@AllArgsConstructor
public class TriageSession implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String sessionNo;
    Integer consultationId;
    Integer accountId;
    Integer patientId;
    String patientName;
    Integer categoryId;
    String categoryName;
    Integer departmentId;
    String departmentName;
    String sourceType;
    String status;
    Integer triageLevelId;
    String triageLevelCode;
    String triageLevelName;
    String triageActionType;
    String triageSummary;
    Integer messageCount;
    Date startedTime;
    Date endedTime;
    Date createTime;
    Date updateTime;
}
