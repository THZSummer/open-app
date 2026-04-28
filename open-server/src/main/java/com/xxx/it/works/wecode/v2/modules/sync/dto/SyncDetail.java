package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据同步详情
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDetail implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订阅关系ID
     */
    private Long id;
    
    /**
     * 同步状态：success, failed, skipped
     */
    private String status;
    
    /**
     * 错误信息（失败时）
     */
    private String error;
    
    /**
     * 审批记录同步状态
     */
    private String approvalStatus;
    
    /**
     * 审批日志同步状态
     */
    private String approvalLogStatus;
}