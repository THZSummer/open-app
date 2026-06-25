package com.xxx.it.works.wecode.v2.modules.flow;

import com.xxx.it.works.wecode.v2.common.enums.ExecutionEnums;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.ExecutionRecordDetailVO;
import com.xxx.it.works.wecode.v2.modules.flow.dto.ExecutionRecordVO;
import com.xxx.it.works.wecode.v2.modules.flow.entity.ExecutionRecord;
import com.xxx.it.works.wecode.v2.modules.flow.entity.ExecutionStep;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.ExecutionRecordMapper;
import com.xxx.it.works.wecode.v2.modules.flow.mapper.ExecutionStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运行记录查询服务（open-server 管理侧）
 *
 * <p>提供执行记录的分页查询和详情查询（含步骤日志）。
 * 所有查询按 appId 做应用数据隔离。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Service
public class ExecutionRecordService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExecutionRecordService.class);




    @Autowired
    public ExecutionRecordService(ExecutionRecordMapper executionRecordMapper, ExecutionStepMapper executionStepMapper) {
        this.executionRecordMapper = executionRecordMapper;
        this.executionStepMapper = executionStepMapper;
    }
    private final ExecutionRecordMapper executionRecordMapper;
    private final ExecutionStepMapper executionStepMapper;

    /**
     * 分页查询运行记录列表
     *
     * @param flowId      连接流ID
     * @param appId       应用ID（数据隔离）
     * @param curPage     当前页码（从1开始）
     * @param pageSize    每页数量
     * @param status      执行状态过滤（可选）
     * @param triggerType 触发方式过滤（可选）
     * @return 分页运行记录列表
     */
    public ApiResponse<List<ExecutionRecordVO>> listRecords(
            Long flowId, Long appId, Integer curPage, Integer pageSize,
            Integer status, Integer triggerType) {

        int page = curPage != null ? curPage : 1;
        int size = pageSize != null ? pageSize : 20;
        int offset = (page - 1) * size;

        // 查询
        List<ExecutionRecord> records = executionRecordMapper.selectList(
                flowId, appId, status, triggerType, null, null, offset, size);
        Long total = executionRecordMapper.countList(
                flowId, appId, status, triggerType, null, null);

        // 转换为 VO
        List<ExecutionRecordVO> vos = new ArrayList<>();
        if (records != null) {
            for (ExecutionRecord record : records) {
                ExecutionRecordVO vo = toListVO(record);

                // 查询步骤数量
                List<ExecutionStep> steps = executionStepMapper.selectByExecutionId(record.getId());
                vo.setStepCount(steps != null ? steps.size() : 0);

                vos.add(vo);
            }
        }

        int totalPages = total != null ? (int) Math.ceil((double) total / size) : 0;

        return ApiResponse.success(vos, ApiResponse.PageResponse.builder()
                .curPage(page)
                .pageSize(size)
                .total(total != null ? total : 0L)
                .totalPages(totalPages)
                .build());
    }

    /**
     * 查询运行记录详情（含步骤日志列表）
     *
     * @param flowId   连接流ID
     * @param recordId 执行记录ID
     * @param appId    应用ID（数据隔离）
     * @return 运行记录详情（含步骤列表）
     */
    public ApiResponse<ExecutionRecordDetailVO> getDetail(Long flowId, Long recordId, Long appId) {
        ExecutionRecord record = executionRecordMapper.selectById(recordId);

        if (record == null) {
            return ApiResponse.error("404", "运行记录不存在", "Execution record not found");
        }
        if (!flowId.equals(record.getFlowId())) {
            return ApiResponse.error("404", "运行记录不属于该连接流", "Execution record does not belong to this flow");
        }
        if (appId != null && !appId.equals(record.getAppId())) {
            return ApiResponse.error("404", "运行记录不属于该应用", "Execution record does not belong to this app");
        }

        ExecutionRecordDetailVO vo = toDetailVO(record);

        // 查询步骤列表
        List<ExecutionStep> steps = executionStepMapper.selectByExecutionId(recordId);
        if (steps != null && !steps.isEmpty()) {
            List<ExecutionRecordDetailVO.ExecutionStepVO> stepVOs = steps.stream()
                    .map(this::toStepVO)
                    .collect(Collectors.toList());
            vo.setSteps(stepVOs);
        } else {
            vo.setSteps(new ArrayList<>());
        }

        return ApiResponse.success(vo);
    }

    // ==================== 转换方法 ====================

    private ExecutionRecordVO toListVO(ExecutionRecord record) {
        ExecutionRecordVO vo = new ExecutionRecordVO();
        vo.setId(String.valueOf(record.getId()));
        vo.setFlowId(String.valueOf(record.getFlowId()));
        vo.setFlowVersionId(record.getFlowVersionId() != null ? String.valueOf(record.getFlowVersionId()) : null);
        vo.setFlowVersionNumber(record.getFlowVersionNumber());
        vo.setFlowNameCn(record.getFlowNameCn());
        vo.setTriggerType(record.getTriggerType());
        vo.setTriggerAccount(record.getTriggerAccount());
        vo.setStatus(record.getStatus());
        vo.setDurationMs(record.getDurationMs());
        vo.setCacheStatus(record.getCacheStatus());
        vo.setErrorCode(record.getErrorCode());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setTriggerTime(record.getTriggerTime());

        // 枚举描述
        if (record.getTriggerType() != null) {
            ExecutionEnums.TriggerType tt = ExecutionEnums.TriggerType.fromValue(record.getTriggerType());
            vo.setTriggerTypeDesc(tt != null ? tt.getDescription() : null);
        }
        if (record.getStatus() != null) {
            ExecutionEnums.ExecutionStatus es = ExecutionEnums.ExecutionStatus.fromValue(record.getStatus());
            vo.setStatusDesc(es != null ? es.getDescription() : null);
        }

        return vo;
    }

    private ExecutionRecordDetailVO toDetailVO(ExecutionRecord record) {
        ExecutionRecordDetailVO vo = new ExecutionRecordDetailVO();
        vo.setId(String.valueOf(record.getId()));
        vo.setFlowId(String.valueOf(record.getFlowId()));
        vo.setFlowVersionId(record.getFlowVersionId() != null ? String.valueOf(record.getFlowVersionId()) : null);
        vo.setFlowVersionNumber(record.getFlowVersionNumber());
        vo.setFlowNameCn(record.getFlowNameCn());
        vo.setFlowNameEn(record.getFlowNameEn());
        vo.setTriggerType(record.getTriggerType());
        vo.setTriggerAccount(record.getTriggerAccount());
        vo.setStatus(record.getStatus());
        vo.setDurationMs(record.getDurationMs());
        vo.setRateLimitStatus(record.getRateLimitStatus());
        vo.setCacheStatus(record.getCacheStatus());
        vo.setCacheKey(record.getCacheKey());
        vo.setCacheTtlRemaining(record.getCacheTtlRemaining());
        vo.setErrorCode(record.getErrorCode());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setTriggerTime(record.getTriggerTime());
        vo.setCreateTime(record.getCreateTime());

        // 枚举描述
        if (record.getTriggerType() != null) {
            ExecutionEnums.TriggerType tt = ExecutionEnums.TriggerType.fromValue(record.getTriggerType());
            vo.setTriggerTypeDesc(tt != null ? tt.getDescription() : null);
        }
        if (record.getStatus() != null) {
            ExecutionEnums.ExecutionStatus es = ExecutionEnums.ExecutionStatus.fromValue(record.getStatus());
            vo.setStatusDesc(es != null ? es.getDescription() : null);
        }

        return vo;
    }

    private ExecutionRecordDetailVO.ExecutionStepVO toStepVO(ExecutionStep step) {
        ExecutionRecordDetailVO.ExecutionStepVO vo = new ExecutionRecordDetailVO.ExecutionStepVO();
        vo.setId(String.valueOf(step.getId()));
        vo.setNodeId(step.getNodeId());
        vo.setNodeType(step.getNodeType());
        vo.setNodeLabelCn(step.getNodeLabelCn());
        vo.setNodeLabelEn(step.getNodeLabelEn());
        vo.setIteration(step.getIteration());
        vo.setStatus(step.getStatus());
        vo.setCacheStatus(step.getCacheStatus());
        vo.setInputData(step.getInputData());
        vo.setOutputData(step.getOutputData());
        vo.setErrorMessage(step.getErrorMessage());
        vo.setErrorCode(step.getErrorCode());
        vo.setDurationMs(step.getDurationMs());
        vo.setCreateTime(step.getCreateTime());

        // 枚举描述
        if (step.getNodeType() != null) {
            ExecutionEnums.NodeType nt = ExecutionEnums.NodeType.fromValue(step.getNodeType());
            vo.setNodeTypeDesc(nt != null ? nt.getDescription() : null);
        }
        if (step.getStatus() != null) {
            ExecutionEnums.ExecutionStatus es = ExecutionEnums.ExecutionStatus.fromValue(step.getStatus());
            vo.setStatusDesc(es != null ? es.getDescription() : null);
        }

        return vo;
    }
}
