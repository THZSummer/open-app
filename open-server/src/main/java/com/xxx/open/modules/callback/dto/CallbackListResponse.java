package com.xxx.open.modules.callback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 回调列表响应
 * 
 * <p>用于回调列表接口返回</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallbackListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 回调ID
     */
    private String id;

    /**
     * 中文名称
     */
    private String nameCn;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 所属分类ID
     */
    private String categoryId;

    /**
     * 所属分类名称
     */
    private String categoryName;

    /**
     * 状态：0=草稿, 1=待审, 2=已发布, 3=已下线
     */
    private Integer status;

    /**
     * 权限信息
     */
    private PermissionDto permission;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 文档链接（从properties中提取）
     */
    private String docUrl;
}
