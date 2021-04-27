package com.atguigu.gmall.index.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {
    /**
     *缓存key的前缀
     * @return
     */
    String prefix() default "gmall:";

    /**
     * 缓存过期时间。默认值是30min
     * 单位min
     * @return
     */
    int timeout() default 30;

    /**
     * 为了防止缓存雪崩，添加随机值范围。默认5min
     * 单位min
     * @return
     */
    int random() default 5;

    /**
     * 为了防止缓存击穿，指定分布式锁的前缀
     * 默认：gmall:lock
     * @return
     */
    String lock() default "gmall:lock";
}
