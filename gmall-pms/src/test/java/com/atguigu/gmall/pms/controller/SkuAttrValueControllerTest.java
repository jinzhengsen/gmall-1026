package com.atguigu.gmall.pms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkuAttrValueControllerTest {

    @Autowired
    private SkuAttrValueController skuAttrValueController;

    @Test
    void querySaleAttrValuesBySkuId() {
        System.out.println(this.skuAttrValueController.querySaleAttrValuesBySkuId(100L).getData());

    }

    @Test
    void testQuerySaleAttrValuesBySkuId() {
    }

    @Test
    void querySaleAttrValuesBySpuId() {
        System.out.println(this.skuAttrValueController.querySaleAttrValuesBySpuId(52L).getData());
    }
}