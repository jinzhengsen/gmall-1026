package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter bloomFilter;


    /**
     * 此通知封装缓存，切点表达式使用的是注解的切点表达式
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取目标方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取目标方法对象
        Method method = signature.getMethod();
        // 获取目标方法上的注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        Class returnType = signature.getReturnType();

        //获取缓存key的前缀
        String prefix = gmallCache.prefix();

        //形参列表
        List<Object> args = Arrays.asList(joinPoint.getArgs());

        // 组装key：prefix + args
        String key=prefix+args;

        // 使用布隆过滤器判定元素是否存在
        if (!this.bloomFilter.contains(key)) {
            return null;
        }

        // 先查询缓存，如果缓存中命中，直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, returnType);
        }

        // 如果缓存中没有，在执行目标方法之前，添加分布式锁（防止缓存击穿）
        String lock = gmallCache.lock() + args;
        RLock fairLock = this.redissonClient.getFairLock(lock);
        fairLock.lock();

        try {
            // 再查询缓存（在获取分布式锁的过程中，可能有其他请求已经把数据放入缓存），命中则直接返回
            String json2 = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(json2)){
                return JSON.parseObject(json2, returnType);
            }

            // 执行目标方法
            Object result = joinPoint.proceed(joinPoint.getArgs());

            // 放入缓存
            if (result != null){
                int timeout = gmallCache.timeout();
                int random = gmallCache.random();
                this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout + new Random().nextInt(random), TimeUnit.MINUTES);
            }

            return result;
        } finally {
            fairLock.unlock();
        }
    }
}
