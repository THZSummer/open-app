package com.xxx.it.works.wecode.v2.modules.flow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 输出映射 (分段结构)
 * <p>
 * 将 outputContract 定义的数据提取并映射到最终响应。
 * header/body 各段独立映射，key 为响应字段路径，value 为输出字段表达式。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutputMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Header 段映射: {输出字段名 -> 源表达式} */
    private Map<String, String> header;

    /** Body 段映射: {输出字段名 -> 源表达式} */
    private Map<String, String> body;
}