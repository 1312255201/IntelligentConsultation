package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class AdminOperationLogPageVO {
    List<AdminOperationLogVO> records;
    Long total;
    Integer pageNo;
    Integer pageSize;
}
