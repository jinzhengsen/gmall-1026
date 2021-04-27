package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BloomFilterConfig {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cates:[";
    private static final String KEY_SUFFIX = "]";

    @Bean
    public RBloomFilter<String> bloomFilter(){
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("index:bloom");
        bloomFilter.tryInit(5000l, 0.03);
        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.queryCategory(0l);
        List<CategoryEntity> categories = responseVo.getData();
        categories.forEach(category -> {
            bloomFilter.add(KEY_PREFIX + category.getId() + KEY_SUFFIX);
        });
        return bloomFilter;
    }
}
