package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuBoundsEntity;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallSmsApi {
    @PostMapping("sms/skubounds/skusale/save")
    public ResponseVo saveSkuSaleInfo(@RequestBody SkuSaleVo skuSaleVo);

   /* @GetMapping("sms/skubounds/{id}")
    public ResponseVo<SkuBoundsEntity> querySkuBoundsById(@PathVariable("id") Long id);*/

    @GetMapping("sms/skubounds/sales/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querySalesBySkuId(@PathVariable("skuId") Long skuId);
}
