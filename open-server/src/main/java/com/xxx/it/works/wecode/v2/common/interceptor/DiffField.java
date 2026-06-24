package com.xxx.it.works.wecode.v2.common.interceptor;

/**
 * diff 字段定义（不可变数据结构）
 *
 * <p>描述"一个字段怎么展示在审计 diff 里"：</p>
 * <ul>
 *   <li>{@link #name}      实体 JSON 中的字段名（如 "appNameCn"）</li>
 *   <li>{@link #labelCn}   中文标签（如 "中文名"）</li>
 *   <li>{@link #labelEn}   英文标签（如 "Chinese name"）</li>
 *   <li>{@link #labelOnly} true=只显示"修改X"不对比值（适用于文件 ID 类字段）</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public record DiffField(String name, String labelCn, String labelEn, boolean labelOnly) {

    /**
     * 普通字段（需对比 before/after 值）
     */
    public static DiffField of(String name, String labelCn, String labelEn) {
        return new DiffField(name, labelCn, labelEn, false);
    }

    /**
     * labelOnly 字段（只显示"修改X"，不对比值，适用于文件 ID 类）
     */
    public static DiffField labelOnly(String name, String labelCn, String labelEn) {
        return new DiffField(name, labelCn, labelEn, true);
    }
}
