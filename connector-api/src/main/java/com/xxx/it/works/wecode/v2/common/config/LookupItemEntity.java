package com.xxx.it.works.wecode.v2.common.config;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity for openplatform_lookup_item_t.
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Table("openplatform_lookup_item_t")
public class LookupItemEntity {

    @Column("item_code")
    private String itemCode;

    @Column("item_value")
    private String itemValue;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }
}
