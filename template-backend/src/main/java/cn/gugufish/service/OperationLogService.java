package cn.gugufish.service;

import cn.gugufish.entity.dto.OperationLog;

public interface OperationLogService {
    void saveLog(OperationLog log);
}
