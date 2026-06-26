package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity for {@code openplatform_property_t} data dictionary table.
 *
 * <p>Used by connector-api to read platform configuration properties
 * (e.g. {@code log_collection_enabled}) without depending on open-server.</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Table("openplatform_property_t")
public class OpenplatformPropertyEntity {

    @Id
    private Long id;

    @Column("path")
    private String path;

    @Column("code")
    private String code;

    @Column("value")
    private String value;

    @Column("status")
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
