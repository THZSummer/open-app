package com.xxx.it.works.wecode.v2.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Connector API platform property service.
 *
 * <p>Reads runtime configuration from {@code openplatform_property_t} via R2DBC.
 * Each property has a hardcoded fallback default; failures to read the DB
 * never throw exceptions or return null.</p>
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
     * Whether step log collection is enabled.
     *
     * <p>Reads {@code connector_platform / log_collection_enabled} from the data dictionary.
     * Defaults to {@code true} (enabled) when the DB is unreachable or the property is missing.</p>
     *
     * @return {@code true} if log collection is enabled, {@code false} otherwise
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
