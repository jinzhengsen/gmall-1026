package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;
/**
 * 接受页面传递过来的检索参数
 * search?keyword=小米&brandId=1,3&cid=225&props=5:高通-麒麟&props=6:骁龙865-硅谷1000&sort=1&priceFrom=1000&priceTo=6000&pageNum=1&store=true
 *
 */
@Data
public class SearchParamVo {
    private String keyword;  //搜索关键字
    private List<Long> brandId; //品牌过滤条件
    private List<Long> categoryId;  //分类过滤条件

    // ["4:8G-12G", "5:128G-256G"]
    private List<String> props; // 规格参数过滤条件

    // 0-综合排序 1-价格降序 2-价格升序 3-新品 4-销量
    private Integer sort; // 排序字段

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

    // 是否有货
    private Boolean store;

    // 分页参数
    private Integer pageNum = 1;
    private final Integer pageSize = 20;
}
