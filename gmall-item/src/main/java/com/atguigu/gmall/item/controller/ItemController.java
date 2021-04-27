package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class ItemController {
    @Autowired
    private ItemService itemService;

    @GetMapping("{skuId}.html")
    public String item(@PathVariable("skuId") Long skuId, Model model){
        ItemVo itemVo=this.itemService.loadData(skuId);
        model.addAttribute("itemVo",itemVo);
        //在返回动态页面给用户之前，生成静态页面
        this.itemService.asyncExecute(skuId);  //因为用户第一次根本不需要得到该静态页面，所以采用异步执行的方式生成静态页面
        return "item";
    }




/*
    @GetMapping("{skuId}")
    public ResponseVo<ItemVo> item(@PathVariable("skuId") Long skuId){
        ItemVo itemVo = this.itemService.loadData(skuId);
        return ResponseVo.ok(itemVo);
    }*/
}
