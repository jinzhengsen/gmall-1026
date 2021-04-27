package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Timer timer;

    public boolean tryLock(String lockName,String uuid,Integer expire){
        String script = "if(redis.call('EXISTS', KEYS[1]) == 0 or redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) " +
                "then " +
                "   redis.call('HINCRBY', KEYS[1], ARGV[1], 1) " +
                "   redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        if (!this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList(lockName),uuid,expire.toString())){
            // 获取锁失败，则重试
            try {
                Thread.sleep(100);
                this.tryLock(lockName,uuid,expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    public void unlock(String lockName,String uuid){
        String script = "if(redis.call('HEXISTS', KEYS[1], ARGV[1]) == 0) " +
                "then " +
                "   return nil " +
                "elseif(redis.call('HINCRBY', KEYS[1], ARGV[1], -1) == 0) " +
                "then " +
                "   return redis.call('DEL', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if (flag==null){
            throw new RuntimeException("你尝试解的锁不属于您");
        }
    }

    public void renewExpire(String lockName, String uuid, Integer expire){
        String script = "if(redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1)" +
                " then return redis.call('expire', KEYS[1], ARGV[2]) " +
                "else return 0 end";
        this.timer=new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class),Arrays.asList(lockName),uuid,expire.toString());
            }
        },expire*1000/3,expire*1000/3);
    }
}
