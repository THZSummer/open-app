package com.xxx.it.works.wecode.v2.modules.sync.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 数据同步请求
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
public class SyncRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订阅关系ID列表
     * null或空数组表示全量同步
     */
    private List<Long> ids;
}