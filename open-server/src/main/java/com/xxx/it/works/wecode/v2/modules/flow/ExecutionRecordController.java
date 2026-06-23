package com.xxx.it.works.wecode.v2.modules.flow;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.flow.dto.ExecutionRecordDetailVO;
import com.xxx.it.works.wecode.v2.modules.flow.dto.ExecutionRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运行记录查询 Controller（V3 open-server 管理侧）
 *
 * <p>API #49~#50：分页查询运行记录列表 + 查看单条执行记录详情（含步骤日志）</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/flows")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Tag(name = "运行记录", description = "连接流运行记录查询接口（列表 + 详情）")
public class ExecutionRecordController {

    private final ExecutionRecordService executionRecordService;

    /**
     * #49 分页查询运行记录列表
     *
     * <p>按 flowId + appId 过滤，支持 status/triggerType 可选过滤，
     * 默认按 triggerTime 倒序排列。</p>
     */
    @GetMapping("/{flowId}/executions")
    @Operation(summary = "#49 查询运行记录列表", description = "分页查询该连接流的执行记录，支持按 status/triggerType 过滤，按 triggerTime 倒序")
    public ApiResponse<List<ExecutionRecordVO>> listExecutionRecords(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "执行状态过滤: 0=成功, 1=失败") @RequestParam(required = false) Integer status,
            @Parameter(description = "触发方式过滤: 1=HTTP触发, 2=调试触发") @RequestParam(required = false) Integer triggerType,
            @Parameter(description = "当前页码") @RequestParam(required = false, defaultValue = "1") Integer curPage,
            @Parameter(description = "每页数量") @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        log.info("GET /flows/{}/executions - list: appId={}, status={}, triggerType={}",
                flowId, appId, status, triggerType);
        return executionRecordService.listRecords(flowId, appId, curPage, pageSize, status, triggerType);
    }

    /**
     * #50 查看单条执行记录详情（含步骤日志）
     *
     * <p>返回完整执行记录信息 + 步骤日志数组。</p>
     */
    @GetMapping("/{flowId}/executions/{executionId}")
    @Operation(summary = "#50 查看运行记录详情", description = "查看单条执行记录详情，含步骤日志数组（节点 I/O 日志内嵌）")
    public ApiResponse<ExecutionRecordDetailVO> getExecutionDetail(
            @RequestHeader("X-App-Id") Long appId,
            @Parameter(description = "连接流ID") @PathVariable Long flowId,
            @Parameter(description = "执行记录ID") @PathVariable Long executionId) {

        log.info("GET /flows/{}/executions/{} - detail: appId={}", flowId, executionId, appId);
        return executionRecordService.getDetail(flowId, executionId, appId);
    }
}
