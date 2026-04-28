package com.campus.annotation;

import java.lang.annotation.*;

/**
 * 自定义日志注解
 * 技术亮点：自定义注解 + AOP 实现声明式日志记录
 * 只需在方法上加 @Log 注解即可自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     * 操作描述
     */
    String value() default "";
}

