package com.xxx.it.works.wecode.v2.modules.auditlog.controller;

import com.xxx.it.works.wecode.v2.common.enums.OperateResultEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.auditlog.entity.OperateLog;
import com.xxx.it.works.wecode.v2.modules.auditlog.mapper.OperateLogMapper;
import com.xxx.it.works.wecode.v2.modules.auditlog.vo.OperateLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 *
 * @author SDDU Build Agent
 * @date 2026-06-07
 */
@Slf4j
@RestController
@RequestMapping("/service/open/v2/operate-log")
public class OperateLogController {

    @Autowired
    private OperateLogMapper operateLogMapper;

    /**
     * 分页查询操作日志
     */
    @GetMapping("")
    public ApiResponse<List<OperateLogVO>> getOperateLogList(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String operateType,
            @RequestParam(required = false) String operateObject,
            @RequestParam(required = false) String operateUser,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        int offset = (curPage - 1) * pageSize;

        List<OperateLog> logs = operateLogMapper.selectPage(
                appId, operateType, operateObject, operateUser, startTime, endTime, offset, pageSize);
        Long total = operateLogMapper.selectPageCount(
                appId, operateType, operateObject, operateUser, startTime, endTime);

        // 转换为前端 VO
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<OperateLogVO> voList = new ArrayList<>();
        for (OperateLog logEntity : logs) {
            OperateLogVO vo = new OperateLogVO();
            vo.setId(logEntity.getId());
            vo.setAccount(logEntity.getOperateUser());
            vo.setOperationType(logEntity.getOperateType());
            vo.setOperationObject(logEntity.getOperateObject());
            vo.setDescription(logEntity.getOperateDescCn());
            vo.setIp(logEntity.getIpAddress());
            vo.setTime(logEntity.getCreateTime() != null ? sdf.format(logEntity.getCreateTime()) : "");
            vo.setResult(logEntity.getStatus() != null && logEntity.getStatus() == OperateResultEnum.SUCCESS.getCode() ? "成功" : "失败");
            voList.add(vo);
        }

        int totalPages = (int) Math.ceil((double) total / pageSize);
        ApiResponse.PageResponse page = ApiResponse.PageResponse.builder()
                .curPage(curPage)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .build();

        return ApiResponse.success(voList, page);
    }

    /**
     * 获取筛选条件选项
     */
    @GetMapping("/filters")
    public ApiResponse<Map<String, Object>> getOperateLogFilters() {
        // 操作类型选项
        List<Map<String, String>> operationTypes = new ArrayList<>();
        operationTypes.add(buildOption("create", "新增"));
        operationTypes.add(buildOption("update", "修改"));
        operationTypes.add(buildOption("delete", "删除"));
        operationTypes.add(buildOption("query", "查询"));

        // 操作对象选项
        List<Map<String, String>> operationObjects = new ArrayList<>();
        operationObjects.add(buildOption("app", "应用"));
        operationObjects.add(buildOption("member", "成员"));
        operationObjects.add(buildOption("api", "API"));
        operationObjects.add(buildOption("event", "事件"));
        operationObjects.add(buildOption("version", "版本"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("operationTypes", operationTypes);
        data.put("operationObjects", operationObjects);

        return ApiResponse.success(data);
    }

    private static Map<String, String> buildOption(String value, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("label", label);
        return m;
    }
}
