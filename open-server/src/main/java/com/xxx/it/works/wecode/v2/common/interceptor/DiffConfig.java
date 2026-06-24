package com.xxx.it.works.wecode.v2.common.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一个操作的完整 diff 配置（模板配置式）
 *
 * <p>包含两部分：</p>
 * <ol>
 *   <li><b>字段定义</b>：{@link #fields()} 声明哪些字段参与 diff、中英文标签、是否 labelOnly</li>
 *   <li><b>格式模板</b>：4 个模板字符串（中/英 × 普通/labelOnly），用占位符表达格式</li>
 * </ol>
 *
 * <p><b>占位符</b>（格式模板中支持）：</p>
 * <ul>
 *   <li>${label}  → 字段标签（按语言取 labelCn / labelEn）</li>
 *   <li>${before} → 修改前值（null 当空串处理）</li>
 *   <li>${after}  → 修改后值（null 当空串处理）</li>
 * </ul>
 *
 * <p><b>默认格式模板</b>（不显式配置时使用）：</p>
 * <table>
 *   <tr><th>场景</th><th>中文</th><th>英文</th></tr>
 *   <tr><td>普通字段</td><td>${label}: 由"${before}"改为"${after}"</td><td>${label}: from "${before}" to "${after}"</td></tr>
 *   <tr><td>labelOnly</td><td>修改${label}</td><td>Modified ${label}</td></tr>
 * </table>
 *
 * <p><b>用法</b>（在 {@code OperateEnum} 枚举值中 override {@code diffConfig()}）：</p>
 * <pre>{@code
 * UPDATE_APP(...) {
 *     @Override
 *     public DiffConfig diffConfig() {
 *         return DiffConfig.builder()
 *             .field("iconId", "应用图标", "App icon", true)
 *             .field("appNameCn", "中文名", "Chinese name")
 *             .field("appNameEn", "英文名", "English name")
 *             // 格式模板不配则用默认值；需要自定义时：
 *             // .formatCnNormal("【${label}】${before} → ${after}")
 *             .build();
 *     }
 * },
 * }</pre>
 *
 * <p><b>设计原则</b>：改 diff 只改配置（枚举常量），不写 Java 实现类，不重新编译格式代码。</p>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public class DiffConfig {

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
     *
     * @param field     字段定义
     * @param beforeVal 修改前值（可为 null）
     * @param afterVal  修改后值（可为 null）
     * @param isChinese true 中文格式，false 英文格式
     * @return 单行 diff 文案
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

        /**
         * 添加普通字段（对比 before/after 值）
         */
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
