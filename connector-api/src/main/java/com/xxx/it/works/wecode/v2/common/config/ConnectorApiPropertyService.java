package com.xxx.it.works.wecode.v2.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 连接器平台属性服务。
 *
 * <p>Reads runtime configuration from {@code openplatform_property_t} via R2DBC.
 * 每个属性都有硬编码的兜底默认值；读取 DB 失败时
 * 不会抛出异常或返回 null。</p>
 *
 * <p>Currently supports:
 * <ul>
 *   <li>{@code log_collection_enabled} — step log collection toggle (default true)</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Service
public class ConnectorApiPropertyService {

    private static final Logger log = LoggerFactory.getLogger(ConnectorApiPropertyService.class);

    private static final String PATH_CONNECTOR_PLATFORM = "connector_platform";

    private final OpenplatformPropertyRepository propertyRepository;

    public ConnectorApiPropertyService(OpenplatformPropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    /**
     * 是否启用步骤日志采集。
     *
     * <p>Reads {@code connector_platform / log_collection_enabled} from the data dictionary.
     * 当 DB 不可达或属性缺失时默认为 {@code true}（启用）。</p>
     *
     * @return 日志采集启用时返回 {@code true}，否则返回 {@code false}
     */
    public boolean isLogCollectionEnabled() {
        try {
            return propertyRepository
                    .findByPathAndCodeAndStatus(PATH_CONNECTOR_PLATFORM, "log_collection_enabled")
                    .map(entity -> {
                        String value = entity.getValue();
                        return value != null && (value.equalsIgnoreCase("true") || value.equals("1"));
                    })
                    .defaultIfEmpty(true)
                    .onErrorReturn(true)
                    .block(); // blocking read is acceptable for a startup/control-plane toggle
        } catch (Exception e) {
            log.warn("Failed to read log_collection_enabled from DB, defaulting to true", e);
            return true;
        }
    }
}
