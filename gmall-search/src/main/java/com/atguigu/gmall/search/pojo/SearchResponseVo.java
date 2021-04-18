package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {
    //品牌列表(返回可选的品牌集合)
    private List<BrandEntity> brands;
    //分类列表(返回种类集合)
    private List<CategoryEntity> categories;
    //规格参数的过滤条件(返回可选参数集合)
    private List<SearchResponseAttrVo> filters;
    //分页数据
    private Integer pageNum;
    private Integer pageSize;
    private Long total;
    // 当前页数据
    private List<Goods> goodsList;
}
