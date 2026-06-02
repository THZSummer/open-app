package com.xxx.it.works.wecode.v2.modules.connector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 契约 Schema (协议感知分段结构)
 * <p>
 * 描述某个端点的请求/响应协议及数据结构。
 * protocol 字段指示协议类型 (如 "HTTP")，
 * header/query/body 分别为各段的数据结构定义。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 协议类型，如 "HTTP" */
    private String protocol;

    /** Header 段数据结构 */
    private ContractBody header;

    /** Query 段数据结构 */
    private ContractBody query;

    /** Body 段数据结构 */
    private ContractBody body;
}