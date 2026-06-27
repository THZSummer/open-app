package com.xxx.it.works.wecode.v2.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

/**
 * 标准环境待实现标记 — 标注当前为 stub/mock 占位，标准环境需补充真实实现。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD, FIELD})
public @interface StandardTodo {
    String value() default "";
}
