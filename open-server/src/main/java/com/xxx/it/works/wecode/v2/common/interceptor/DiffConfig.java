package com.xxx.it.works.wecode.v2.common.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 操作 diff 配置（模板配置式）
 *
 * <p>占位符：${label}、${before}、${after}</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public class DiffConfig {

    /**
     * 模板中字段级 diff 的占位符（OperateEnum 模板和 OperateLogV2Aspect 渲染统一引用）
     */
    public static final String DIFF_FIELDS_PLACEHOLDER = "${diffFields}";

    // ===== 默认格式模板 =====
    public static final String DEFAULT_FORMAT_CN_NORMAL = "${label}: 由\"${before}\"改为\"${after}\"";
    public static final String DEFAULT_FORMAT_CN_LABEL_ONLY = "修改${label}";
    public static final String DEFAULT_FORMAT_EN_NORMAL = "${label}: from \"${before}\" to \"${after}\"";
    public static final String DEFAULT_FORMAT_EN_LABEL_ONLY = "Modified ${label}";
    public static final String DEFAULT_SEPARATOR = "\n";

    private final List<DiffField> fields;
    private final String formatCnNormal;
    private final String formatCnLabelOnly;
    private final String formatEnNormal;
    private final String formatEnLabelOnly;
    private final String separator;

    private DiffConfig(Builder b) {
        this.fields = Collections.unmodifiableList(new ArrayList<>(b.fields));
        this.formatCnNormal = b.formatCnNormal != null ? b.formatCnNormal : DEFAULT_FORMAT_CN_NORMAL;
        this.formatCnLabelOnly = b.formatCnLabelOnly != null ? b.formatCnLabelOnly : DEFAULT_FORMAT_CN_LABEL_ONLY;
        this.formatEnNormal = b.formatEnNormal != null ? b.formatEnNormal : DEFAULT_FORMAT_EN_NORMAL;
        this.formatEnLabelOnly = b.formatEnLabelOnly != null ? b.formatEnLabelOnly : DEFAULT_FORMAT_EN_LABEL_ONLY;
        this.separator = b.separator != null ? b.separator : DEFAULT_SEPARATOR;
    }

    public List<DiffField> fields() {
        return fields;
    }

    public String separator() {
        return separator;
    }

    /**
     * 渲染单个字段的 diff 行
     */
    public String renderField(DiffField field, String beforeVal, String afterVal, boolean isChinese) {
        String label = isChinese ? field.labelCn() : field.labelEn();
        String template = field.labelOnly()
                ? (isChinese ? formatCnLabelOnly : formatEnLabelOnly)
                : (isChinese ? formatCnNormal : formatEnNormal);
        return template
                .replace("${label}", label != null ? label : "")
                .replace("${before}", beforeVal != null ? beforeVal : "")
                .replace("${after}", afterVal != null ? afterVal : "");
    }

    /**
     * 构建 DiffConfig
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<DiffField> fields = new ArrayList<>();
        private String formatCnNormal;
        private String formatCnLabelOnly;
        private String formatEnNormal;
        private String formatEnLabelOnly;
        private String separator;

    /** 添加普通字段（对比 before/after 值） */
    public Builder field(String name, String labelCn, String labelEn) {
            fields.add(DiffField.of(name, labelCn, labelEn));
            return this;
        }

        /**
         * 添加 labelOnly 字段（只显示"修改X"，不对比值）
         */
        public Builder labelOnlyField(String name, String labelCn, String labelEn) {
            fields.add(DiffField.labelOnly(name, labelCn, labelEn));
            return this;
        }

        /**
         * 自定义中文普通字段格式模板
         */
        public Builder formatCnNormal(String fmt) {
            this.formatCnNormal = fmt;
            return this;
        }

        /**
         * 自定义中文 labelOnly 格式模板
         */
        public Builder formatCnLabelOnly(String fmt) {
            this.formatCnLabelOnly = fmt;
            return this;
        }

        /**
         * 自定义英文普通字段格式模板
         */
        public Builder formatEnNormal(String fmt) {
            this.formatEnNormal = fmt;
            return this;
        }

        /**
         * 自定义英文 labelOnly 格式模板
         */
        public Builder formatEnLabelOnly(String fmt) {
            this.formatEnLabelOnly = fmt;
            return this;
        }

        /**
         * 自定义多行分隔符（默认换行）
         */
        public Builder separator(String sep) {
            this.separator = sep;
            return this;
        }

        public DiffConfig build() {
            return new DiffConfig(this);
        }
    }
}
