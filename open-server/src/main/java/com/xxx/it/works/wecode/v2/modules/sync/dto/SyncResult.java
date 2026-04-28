package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 数据同步结果
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 成功数量
     */
    private int success;
    
    /**
     * 失败数量
     */
    private int failed;
    
    /**
     * 跳过数量（已存在）
     */
    private int skipped;
    
    /**
     * 详细结果列表
     */
    private List<SyncDetail> details;
}