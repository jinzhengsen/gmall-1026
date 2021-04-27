package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkuAttrValueMapperTest {
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Test
    void querySaleAttrValuesMappingSkuIdBySkuIds() {
        List<Long> skuIds = Arrays.asList(100L, 101L);
        System.out.println(this.skuAttrValueMapper.querySaleAttrValuesMappingSkuIdBySkuIds(skuIds));
    }
}