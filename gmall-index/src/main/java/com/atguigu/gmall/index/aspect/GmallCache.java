package com.atguigu.gmall.index.aspect;

import org.springframework.transaction.TransactionDefinition;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * key：prefix + ":" + 方法参数
     * @return
     */
    String prefix() default "";

    /**
     * 指定缓存时间
     * 单位是分钟
     * 默认是5分钟
     * @return
     */
    int timeout() default 5;

    /**
     * 为了防止缓存雪崩
     * 让注解使用人员指定时间随机值范围
     * 默认是5分钟
     * @return
     */
    int random() default 5;

    /**
     * 为了防止缓存击穿
     * 让注解使用人员指定分布式锁key的前缀
     * 默认是lock
     * key: lock + ":" + 方法参数
     * @return
     */
    String lock() default "lock";
}
