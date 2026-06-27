package com.xxx.it.works.wecode.v2.modules.connectorversion.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxx.it.works.wecode.v2.modules.connector.model.ConnectionConfig;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;

/**
 * 连接器版本/配置实体
 * <p>
 * 对应表 openplatform_v2_cp_connector_version_t
 * V3 多版本模型: 1:N connector_t，每实体最多 1000 个版本
 * V3 新增: version_number, status, published_time, published_by
 * </p>
 */
@Data
public class ConnectorVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ConnectorVersion.class);

    /** 雪花ID (应用层生成) */
    private Long id;

    /** 关联连接器ID (逻辑外键 → connector_t.id) */
    private Long connectorId;

    /** 版本号，实体内从1递增（V3 新增） */
    private Integer versionNumber;

    /** 状态: 1=草稿, 2=已发布, 3=已失效, 4=物理删除（V3 新增） */
    private Integer status;

    /** 连接配置JSON (完整 connection_config 字符串) */
    private String connectionConfig;

    /** 发布时间（首次发布时刻，V3 新增） */
    private Date publishedTime;

    /** 发布人（发布操作人，V3 新增） */
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
     * 获取类型化的 ConnectionConfig 对象
     * <p>
     * 对 connectionConfig 字符串做 Jackson 反序列化，
     * 使用 v5.5 POJO 结构。线程不安全，需每次调用重新解析。
     * </p>
     */
    public ConnectionConfig getConnectionConfigObj() {
        if (this.connectionConfig == null || this.connectionConfig.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(this.connectionConfig, ConnectionConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize connectionConfig: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 设置类型化的 ConnectionConfig 对象
     * <p>
     * 将 ConnectionConfig POJO 序列化为 JSON 字符串存入 connectionConfig 字段。
     * </p>
     */
    public void setConnectionConfigObj(ConnectionConfig config) {
        if (config == null) {
            this.connectionConfig = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            this.connectionConfig = mapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize connectionConfig: {}", e.getMessage(), e);
            this.connectionConfig = null;
        }
    }
}
