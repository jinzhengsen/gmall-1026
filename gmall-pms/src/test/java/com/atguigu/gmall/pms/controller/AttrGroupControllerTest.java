package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AttrGroupControllerTest {
    @Autowired
    private AttrGroupController attrGroupController;

    @Test
    void queryGroupWithAttrValuesByCidAndSpuIdAndSkuId() {
        List<ItemGroupVo> itemGroupVos = this.attrGroupController.queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(225L, 52L, 100L).getData();
        for (int i = 0; i < itemGroupVos.size(); i++) {
            System.out.println(itemGroupVos.get(i));
        }
    }
}