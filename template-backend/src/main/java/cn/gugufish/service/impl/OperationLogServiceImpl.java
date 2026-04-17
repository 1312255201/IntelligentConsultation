package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.OperationLog;
import cn.gugufish.mapper.OperationLogMapper;
import cn.gugufish.service.OperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public void saveLog(OperationLog log) {
        if (log == null) return;
        this.save(log);
    }
}
