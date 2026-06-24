package com.xxx.it.works.wecode.v2.modules.app.resolver;

import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用上下文
 *
 * <p>包含解析后的应用信息，用于业务逻辑处理</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Data
@Builder
public class AppContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 内部主键ID（用于数据库查询）
     */
    private Long internalId;

    /**
     * 外部业务ID（用于响应返回）
     */
    private String externalId;

    /**
     * 应用实体（resolveAndValidate 已查询，避免调用方重复查询）
     */
    private App app;

    public AppContext() {
    }

    public Long getInternalId() {
        return this.internalId;
    }

    public void setInternalId(Long internalId) {
        this.internalId = internalId;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public App getApp() {
        return this.app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public static AppContextBuilder builder() {
        return new AppContextBuilder();
    }

    public static class AppContextBuilder {
        private Long internalId;
        private String externalId;
        private App app;

        AppContextBuilder() {
        }

        public AppContextBuilder internalId(Long internalId) {
            this.internalId = internalId;
            return this;
        }

        public AppContextBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public AppContextBuilder app(App app) {
            this.app = app;
            return this;
        }

        public AppContext build() {
            AppContext context = new AppContext();
            context.setInternalId(internalId);
            context.setExternalId(externalId);
            context.setApp(app);
            return context;
        }
    }
}