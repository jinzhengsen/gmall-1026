package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {
    //一二三级分类
    private List<CategoryEntity> categories;

    // 品牌 V
    private Long brandId;
    private String brandName;

    // spu相关信息 V
    private Long spuId;
    private String spuName;


    // sku相关信息 V
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;

    // 是否有货 V
    private Boolean store=false;
    // 营销信息 V
    private List<ItemSaleVo> sales;
    // 图片列表 V
    private List<SkuImagesEntity> images;

    // 销售属性: V
    // [{attrId: 3, attrName: 颜色, attrValues: ['暗夜黑', '白天白']},
    // {attrId: 4, attrName: 内存, attrValues: ['8G', '12G']},
    // {attrId: 5, attrName: 存储, attrValues: ['256G', '512G']}]
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性：{3: '白天白', 4: '8G', 5: '512G'} V
    private Map<Long, String> saleAttr;

    // 销售属性组合与skuId的对应关系：{'白天白,8G,256G': 10, '白天白,8G,512G': 11} V
    private String skuJsons;

    // 商品描述信息
    private List<String> spuImages;

    // 规格参数分组及组下的规格参数和值
    private List<ItemGroupVo> groups;
}
