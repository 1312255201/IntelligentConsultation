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
@TableName("db_patient_profile")
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer accountId;
    String name;
    String gender;
    Date birthDate;
    String phone;
    String idCard;
    String relationType;
    Integer isSelf;
    Integer isDefault;
    String remark;
    Integer status;
    Date createTime;
    Date updateTime;
}
