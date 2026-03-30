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
@TableName("db_triage_case_reference")
@NoArgsConstructor
@AllArgsConstructor
public class TriageCaseReference implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String chiefComplaint;
    String symptomSummary;
    String triageResult;
    Integer departmentId;
    Integer doctorId;
    String riskLevel;
    String tags;
    String sourceType;
    Integer sort;
    Integer status;
    Date createTime;
    Date updateTime;
}
