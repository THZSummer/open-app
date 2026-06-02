package com.xxx.api.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * content JSON 字符串反序列化对象
 *
 * <p>ApprovalCallbackRequest.content 字段的 JSON 结构</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCardContent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 卡片链接
     */
    private String url;

    /**
     * 内容类型
     */
    private String type;

    /**
     * 卡片标题
     */
    private String title;

    /**
     * 动作描述
     */
    private String verb;

    /**
     * 审批动作："1"=同意, "0"=驳回
     */
    private String data;
}
