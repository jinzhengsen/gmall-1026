package com.atguigu.gmall.item.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemServiceTest {
    @Autowired
    private ItemService itemService;

    @Test
    void test1() {
        this.itemService.test(151L);
    }

    @Test
    void querySaleAttrValuesBySpuIdTest() {
        this.itemService.querySaleAttrValuesBySpuIdTest(52L);
    }
}