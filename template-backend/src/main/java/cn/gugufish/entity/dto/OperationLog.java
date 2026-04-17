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
@TableName("db_operation_log")
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    Long requestId;
    String requestUrl;
    String requestMethod;
    String remoteIp;
    Integer accountId;
    String username;
    String role;
    String requestParams;
    Integer responseCode;
    String responseSummary;
    Integer durationMs;
    Date createTime;
}
