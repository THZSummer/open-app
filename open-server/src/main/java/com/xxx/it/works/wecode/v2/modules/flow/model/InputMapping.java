package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 输入映射 (分段结构)
 * <p>
 * 将上游输出映射到当前节点的 HTTP 请求分段。
 * header/query/body 各段独立映射，key 为输出字段表达式，value 为请求字段名。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Header 段映射: {源表达式 -> 目标字段名} */
    private Map<String, String> header;

    /** Query 段映射: {源表达式 -> 目标字段名} */
    private Map<String, String> query;

    /** Body 段映射: {源表达式 -> 目标字段名} */
    private Map<String, String> body;
}