package com.xxx.it.works.wecode.v2.modules.flowversion.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.flow.model.OrchestrationConfig;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接流版本/配置实体
 * <p>
 * 对应表 openplatform_v2_cp_flow_version_t
 * V3 多版本模型: 1:N flow_t，每实体最多 1000 个版本
 * V3 新增: version_number, status（7 状态含审批中间态）, published_time, published_by
 * </p>
 */
@Data
public class FlowVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FlowVersion.class);

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 关联连接流ID (逻辑外键 → flow_t.id) */
    private Long flowId;

    /** 版本号，实体内从1递增（V3 新增） */
    private Integer versionNumber;

    /** 状态: 1=草稿, 2=待审批, 3=已撤回, 4=已驳回, 5=已发布, 6=已失效, 7=物理删除（V3 新增 7 状态） */
    private Integer status;

    /** 编排配置JSON (React Flow 格式 + flowConfig) */
    private String orchestrationConfig;

    /** 发布时间（审批通过的时刻，V3 新增） */
    private Date publishedTime;

    /** 发布人（提交审批的人，V3 新增） */
    private String publishedBy;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date lastUpdateTime;

    /** 创建人 */
    private String createBy;

    /** 最后更新人 */
    private String lastUpdateBy;

    /**
     * 获取类型化的 OrchestrationConfig 对象
     * <p>
     * 对 orchestrationConfig 字符串做 Jackson 反序列化，
     * 使用 React Flow POJO 结构。线程不安全，需每次调用重新解析。
     * </p>
     */
    public OrchestrationConfig getOrchestrationConfigObj() {
        if (this.orchestrationConfig == null || this.orchestrationConfig.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(this.orchestrationConfig, OrchestrationConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize orchestrationConfig: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 设置类型化的 OrchestrationConfig 对象
     * <p>
     * 将 OrchestrationConfig POJO 序列化为 JSON 字符串存入 orchestrationConfig 字段。
     * </p>
     */
    public void setOrchestrationConfigObj(OrchestrationConfig config) {
        if (config == null) {
            this.orchestrationConfig = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            this.orchestrationConfig = mapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize orchestrationConfig: {}", e.getMessage(), e);
            this.orchestrationConfig = null;
        }
    }
}
