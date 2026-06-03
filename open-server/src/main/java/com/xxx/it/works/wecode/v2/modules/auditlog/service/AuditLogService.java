package com.xxx.it.works.wecode.v2.modules.auditlog.service;

import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.mapper.OperateLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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
@RequiredArgsConstructor
public class AuditLogService {

    private final OperateLogMapper operateLogMapper;
    private final IdGeneratorStrategy idGenerator;

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
            // 填充主键 ID
            if (operateLog.getId() == null) {
                operateLog.setId(idGenerator.nextId());
            }

            // 填充时间戳
            Date now = new Date();
            if (operateLog.getCreateTime() == null) {
                operateLog.setCreateTime(now);
            }
            if (operateLog.getLastUpdateTime() == null) {
                operateLog.setLastUpdateTime(now);
            }

            // 填充操作人（如果未设置则使用 operateUser）
            if (operateLog.getCreateBy() == null || operateLog.getCreateBy().isEmpty()) {
                operateLog.setCreateBy(operateLog.getOperateUser());
            }
            if (operateLog.getLastUpdateBy() == null || operateLog.getLastUpdateBy().isEmpty()) {
                operateLog.setLastUpdateBy(operateLog.getOperateUser());
            }

            // 插入数据库
            operateLogMapper.insert(operateLog);

            log.debug("[AUDIT] Saved: id={}, type={}, object={}, user={}",
                    operateLog.getId(),
                    operateLog.getOperateType(),
                    operateLog.getOperateObject(),
                    operateLog.getOperateUser());

        } catch (Exception e) {
            // 审计日志写入失败不应影响主业务，仅记录错误日志
            log.error("[AUDIT] Failed to save audit log: type={}, object={}, user={}",
                    operateLog.getOperateType(),
                    operateLog.getOperateObject(),
                    operateLog.getOperateUser(), e);
        }
    }
}
