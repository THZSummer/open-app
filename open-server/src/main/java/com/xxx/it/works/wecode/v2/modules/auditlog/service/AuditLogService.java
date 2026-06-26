package com.xxx.it.works.wecode.v2.modules.auditlog.service;

import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.mapper.OperateLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志服务
 *
 * <p>提供审计日志的异步持久化能力，使用独立事务确保不影响主业务流程</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class AuditLogService {

    @Autowired
    private OperateLogMapper operateLogMapper;
    @Autowired
    private IdGeneratorStrategy idGenerator;

    /**
     * 异步保存审计日志
     *
     * <p>设计要点：</p>
     * <ul>
     *   <li>@Async：使用 auditLogExecutor 线程池异步执行，不阻塞主请求</li>
     *   <li>REQUIRES_NEW：独立事务，审计日志写入失败不会回滚主业务</li>
     *   <li>异常处理：内部 catch 所有异常，仅记录 log.error()，不向调用方传播</li>
     * </ul>
     *
     * @param operateLog 审计日志实体
     */
    @Async("auditLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveAsync(OperateLog operateLog) {
        try {
            if (operateLog.getId() == null) {
                operateLog.setId(idGenerator.nextId());
            }
            operateLogMapper.insert(operateLog);
            log.debug("[AUDIT] Saved: id={}, type={}, object={}, user={}",
                    operateLog.getId(), operateLog.getOperateType(),
                    operateLog.getOperateObject(), operateLog.getOperateUser());
        } catch (Exception e) {
            log.error("[AUDIT] Failed to save audit log: type={}, object={}, user={}",
                    operateLog.getOperateType(), operateLog.getOperateObject(),
                    operateLog.getOperateUser(), e);
        }
    }
}
