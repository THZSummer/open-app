package com.xxx.it.works.wecode.v2.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

/**
 * 标准环境待实现标记 — 标注当前为开发环境 stub/mock 占位，标准环境需补充真实实现。
 *
 * <p>使用方式：
 * <ul>
 *   <li>标注在类上：整个类为标准环境待实现</li>
 *   <li>标注在方法上：该方法为标准环境待实现</li>
 *   <li>标注在字段上：该注入为标准环境待替换</li>
 * </ul>
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD, FIELD})
public @interface StandardTodo {
    /** 待实现说明 */
    String value() default "";
}
