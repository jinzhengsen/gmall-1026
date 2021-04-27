package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock lock;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX="index:cates:";

    private static final String LOCK_PREFIX = "index:cates:lock:";

    public List<CategoryEntity> queryLvl1Categories() {
        //先从缓存中获取以及分类，如果redis中有缓存一级分类，就直接返回数据
        String catesFirst = this.redisTemplate.opsForValue().get(KEY_PREFIX + 0);//得到的数据是json字符串的形式
        if (StringUtils.isNotBlank(catesFirst)){
            List<CategoryEntity> categoryEntities = JSON.parseArray(catesFirst, CategoryEntity.class);
            return categoryEntities;
        }
        //redis中没有就远程调用gmallPmsClient中的方法向数据库中查询出以及分类，并将这些一级分类存入redis中
        ResponseVo<List<CategoryEntity>> catesResponseVo = this.gmallPmsClient.queryCategory(0L);
        List<CategoryEntity> cates = catesResponseVo.getData();
        if (CollectionUtils.isEmpty(cates)){
            // 为了防止缓存穿透，数据即使为null也缓存，缓存时间不超过5min
            this.redisTemplate.opsForValue().set(KEY_PREFIX+0,JSON.toJSONString(cates),5,TimeUnit.MINUTES);
        }else {
            // 为了防止缓存雪崩，给缓存时间添加随机值
            this.redisTemplate.opsForValue().set(KEY_PREFIX+0,JSON.toJSONString(cates),30,TimeUnit.DAYS);
        }
        return cates;
    }


    @GmallCache(prefix = KEY_PREFIX,timeout = 259200,random = 43200,lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLvl2WithSubsByPid(Long pid) {


            ResponseVo<List<CategoryEntity>> catesResponseVo = this.gmallPmsClient.queryLvl2WithSubsByPid(pid);
            List<CategoryEntity> categoryEntities = catesResponseVo.getData();

            return categoryEntities;

    }

    public List<CategoryEntity> queryLvl2WithSubsByPid2(Long pid) {
        //先从缓存中获取，如果缓存中有，直接获取
        String jsonCacheCategories =this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(jsonCacheCategories)){
            //如果缓存中有直接返回
            List<CategoryEntity> categoryEntities = JSON.parseArray(jsonCacheCategories, CategoryEntity.class);
            return categoryEntities;
        }

        // 为了防止缓存击穿，添加分布式锁
        RLock lock = this.redissonClient.getLock("index:cates:lock:" + pid);
        lock.lock();

        try {
            //当发生了缓存穿透时，第一次如果从数据库中查到了，就将数据放入到缓存中，后续直接从缓存中读取就行了，所以再查一次缓存
            String jsonCacheCategories2 =this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(jsonCacheCategories)){
                //如果缓存中有直接返回
                List<CategoryEntity> categoryEntities = JSON.parseArray(jsonCacheCategories, CategoryEntity.class);
                return categoryEntities;
            }
            // 2.缓存中没有命中，远程调用获取数据，并放入缓存
            ResponseVo<List<CategoryEntity>> catesResponseVo = this.gmallPmsClient.queryLvl2WithSubsByPid(pid);
            List<CategoryEntity> categoryEntities = catesResponseVo.getData();
            //把查询结果放入缓存
            if (CollectionUtils.isEmpty(categoryEntities)){
                // 为了防止缓存穿透，数据即使为null也缓存，缓存时间不超过5min
                this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),5, TimeUnit.MINUTES);
            }else {
                // 为了防止缓存雪崩，给缓存时间添加随机值
                this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryEntities),30+new Random().nextInt(10),TimeUnit.DAYS);
            }
            return categoryEntities;
        } finally {
            lock.unlock();
        }
    }

    /*public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
        System.out.println("定时任务初始化时间:"+System.currentTimeMillis());
        scheduledExecutorService.schedule(()->{
            System.out.println("定时执行时间："+System.currentTimeMillis());
        },10,TimeUnit.SECONDS);
    }*/

    public void testLock() {

        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(50,TimeUnit.SECONDS);


        try {
            //执行业务操作
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)){
                this.redisTemplate.opsForValue().set("num","1");
                return;
            }

           /* try {
                TimeUnit.SECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
        } finally {
            lock.unlock();
        }


    }



    public void testLock3() {

            String uuid=UUID.randomUUID().toString();
        boolean lockFlag = this.lock.tryLock("lock", uuid, 30);
        if (lockFlag){
            //执行业务操作
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)){
                this.redisTemplate.opsForValue().set("num","1");
                return;
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));

            //加一个可重入锁
            this.subTestLock(uuid);

            lock.unlock("lock",uuid);

        }
    }

    public void subTestLock(String uuid){
        lock.tryLock("lock",uuid,30);
        System.out.println("子方法执行自己的业务");
        lock.unlock("lock",uuid);
    }


    public void testLock2() {
        //加锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if (!lock){
            //如果获取锁失败，则重试
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {

            //执行业务操作
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)){
                this.redisTemplate.opsForValue().set("num","1");
                return;
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));

            // 释放锁。为了防止误删，要判断是否自己的锁
            String script= "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"),uuid);

       /* if (StringUtils.equals(uuid,this.redisTemplate.opsForValue().get("lock"))){
            this.redisTemplate.delete("lock");
        }*/

        }
    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);
        System.out.println("读业务操作");
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10,TimeUnit.SECONDS);
        System.out.println("写业务操作");
    }

    public void testLatch() {
        try {
            RCountDownLatch cdl = this.redissonClient.getCountDownLatch("cdl");
            cdl.trySetCount(6);
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCountDown() {
        RCountDownLatch cdl = this.redissonClient.getCountDownLatch("cdl");
        cdl.countDown();
    }

  /*  public static void main(String[] args) {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 20, 0.3);
        bloomFilter.put("1");
        bloomFilter.put("2");
        bloomFilter.put("3");
        bloomFilter.put("4");
        bloomFilter.put("5");
        bloomFilter.put("6");
        bloomFilter.put("7");
        System.out.println(bloomFilter.mightContain("1"));
        System.out.println(bloomFilter.mightContain("3"));
        System.out.println(bloomFilter.mightContain("5"));
        System.out.println(bloomFilter.mightContain("7"));
        System.out.println(bloomFilter.mightContain("9"));
        System.out.println(bloomFilter.mightContain("10"));
        System.out.println(bloomFilter.mightContain("11"));
        System.out.println(bloomFilter.mightContain("12"));
        System.out.println(bloomFilter.mightContain("13"));
        System.out.println(bloomFilter.mightContain("14"));
        System.out.println(bloomFilter.mightContain("15"));
        System.out.println(bloomFilter.mightContain("16"));
        System.out.println(bloomFilter.mightContain("17"));
        System.out.println(bloomFilter.mightContain("18"));
        System.out.println(bloomFilter.mightContain("19"));
        System.out.println(bloomFilter.mightContain("20"));
        System.out.println(bloomFilter.mightContain("21"));
    }*/
}
