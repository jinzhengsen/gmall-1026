package com.atguigu.gmall.index.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AspectConfig {
    @Pointcut("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void pointCut(){

    }

//    @Before("pointCut()")
    @AfterReturning("pointCut()")
    public void test(JoinPoint joinPoint){
         joinPoint.getArgs();
    }
}
