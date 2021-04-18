package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.filter.IFilterConfig;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    RestHighLevelClient restHighLevelClient;
    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            //构建查询条件
            SearchRequest searchRequest= new SearchRequest(new String[]{"goods"},this.buildDsl(searchParamVo));
            //执行查询
            SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果集
            SearchResponseVo searchResponseVo = this.parseResult(searchResponse);
            searchResponseVo.setPageNum(searchParamVo.getPageNum());
            searchResponseVo.setPageSize(searchParamVo.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 解析搜索结果集
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse searchResponse){
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        //总命中的记录数
        searchResponseVo.setTotal(hits.getTotalHits());

        SearchHit[] hitsHits = hits.getHits();  //得到的是es查询出来的goods商品信息集合(json)格式的
        searchResponseVo.setGoodsList(Stream.of(hitsHits).map(hitsHit->{
            // 获取内层hits的_source 数据
            String goodsJson = hitsHit.getSourceAsString();
            //反序列化为goods对象
            Goods goods = JSON.parseObject(goodsJson, Goods.class);
            // 获取高亮的title覆盖掉普通title
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            String highlightTitle = highlightField.getFragments()[0].toString();
            goods.setTitle(highlightTitle);
            return goods;
        }).collect(Collectors.toList()));

        //聚合结果的解析
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        // 1. 解析聚合结果集，获取品牌》
        // {attrId: null, attrName: "品牌"， attrValues: [{id: 1, name: 尚硅谷, logo: http://www.atguigu.com/logo.gif}, {}]}
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets(); //得到根据品牌id划分的桶
        if (!CollectionUtils.isEmpty(brandIdAggBuckets)){
            searchResponseVo.setBrands(
                    brandIdAggBuckets.stream().map(brandIdAggBucket->{  // {id: 1, name: 尚硅谷, logo: http://www.atguigu.com/logo.gif}
                        // 为了得到指定格式的json字符串，创建了一个map
                        BrandEntity brandEntity = new BrandEntity();
                        // 获取brandIdAgg中的key，这个key就是品牌的id
                        long brandId = brandIdAggBucket.getKeyAsNumber().longValue();
                        brandEntity.setId(brandId);
                        // 解析品牌名称的子聚合，获取品牌名称
                        Map<String, Aggregation> brandAggregationMap = ((Terms.Bucket)brandIdAggBucket).getAggregations().asMap();
                        ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandAggregationMap.get("brandNameAgg");
                        brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                        // 解析品牌logo的子聚合，获取品牌 的logo
                        ParsedStringTerms logoAgg = (ParsedStringTerms) brandAggregationMap.get("logoAgg");
                        List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(logoAggBuckets)){
                            brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                        }
                        // 把map反序列化为json字符串
                        return brandEntity;
                    }).collect(Collectors.toList())
            );
        }

        //2、解析聚合结果集，获取分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();  //得到根据categoryId分类的桶
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            List<CategoryEntity> categoryEntities = categoryIdAggBuckets.stream().map(bucket -> {   //遍历桶中的元素
                CategoryEntity categoryEntity = new CategoryEntity();
                long categoryId = bucket.getKeyAsNumber().longValue();
                categoryEntity.setId(categoryId);
                ParsedStringTerms categoryNameAgg = ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList());
            searchResponseVo.setCategories(categoryEntities);
        }

        // 3. 解析聚合结果集，获取规格参数:嵌套
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        // 获取嵌套聚合中的attrIdAgg
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        // 获取规格参数id组成桶，由此获取规格参数的个数
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        // 把桶集合转化成List<SearchResponseAttrVo>
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)){
            List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAggBuckets.stream().map(bucket -> {  // 把每个桶转化成SearchResponseAttrVo
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();  // 初始化一个vo对象
                // 1.设置了attrId
                long attrId = bucket.getKeyAsNumber().longValue();
                searchResponseAttrVo.setAttrId(attrId);

                // 2.设置了attrName
                ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");  // 获取了name子聚合
                List<? extends Terms.Bucket> buckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(buckets)) {
                    searchResponseAttrVo.setAttrName(buckets.get(0).getKeyAsString());
                }

                // 3.设置了attrValues
                ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");  // 获取了values子聚合
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueAggBuckets)) {
                    // 把桶集合转化成List<String>集合（规格参数值集合）
                    searchResponseAttrVo.setAttrValues(attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            searchResponseVo.setFilters(searchResponseAttrVos);
        }
        return searchResponseVo;
    }


    private SearchSourceBuilder buildDsl(SearchParamVo searchParamVo){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        String keyword = searchParamVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // TODO: 打广告
            return searchSourceBuilder;
        }
        //1、构建查询过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        searchSourceBuilder.query(boolQueryBuilder);

        //1.1.构建查询条件：匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));

        //1.2.构建过滤条件
        //1.2.1品牌过滤
        List<Long> brandId = searchParamVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }

        //1.2.2.分类过滤
        List<Long> categoryId = searchParamVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }

        //1.2.3.价格区间过滤
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
            // 如果任何一个值不为空，就应该构建范围查询
        if (priceFrom!=null||priceTo!=null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            // 如果from不为空，添加gte
            if (priceFrom!=null){
                rangeQuery.gte(priceFrom);
            }
            // 如果to不为空，添加lte
            if (priceTo!=null){
                rangeQuery.lte(priceTo);
            }
        }

        // 1.2.4. 是否有货过滤
        Boolean store = searchParamVo.getStore();
        if (store!=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }

        // 1.2.5. 规格参数的嵌套过滤: ["4:8G-12G", "5:128G-256G"]
        List<String> props = searchParamVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            // 遍历每一个规格参数的过滤条件，添加嵌套过滤
            props.forEach(prop->{// 4:8G-12G
                if (StringUtils.isNotBlank(prop)){  // 判断当前prop字符串是否为空
                    String[] attr = StringUtils.split(prop, ":");
                    if (attr!=null && attr.length==2){  // 判断分割后的数组的长度是否为两位：1：attrId 2：8G-12G
                        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                        boolQuery.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                        boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attr[0]));
                        // 把值以"-"分割
                        String[] values = StringUtils.split(attr[1], "-");
                        boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",values));
                    }
                }
            });
        }

        // 2.构建排序条件: 0-综合排序 1-价格降序 2-价格升序 3-新品 4-销量
        Integer sort = searchParamVo.getSort();
        if (sort!=null){
            switch (sort){
                case 1: searchSourceBuilder.sort("price", SortOrder.DESC); break;
                case 2: searchSourceBuilder.sort("price", SortOrder.ASC); break;
                case 3: searchSourceBuilder.sort("createTime", SortOrder.DESC); break;
                case 4: searchSourceBuilder.sort("sales", SortOrder.DESC); break;
                default:
                    searchSourceBuilder.sort("_score", SortOrder.DESC);
                    break;
            }
        }
        // 3.构建分页条件
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        searchSourceBuilder.from((pageNum - 1) * pageSize);
        searchSourceBuilder.size(pageSize);

        // 4.构建高亮
        searchSourceBuilder.highlighter(
                new HighlightBuilder()
                        .field("title")
                        .preTags("<font style='color:red'>")
                        .postTags("</font>")
        );

        // 5.构建聚合
        // 5.1. 品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));

        // 5.2. 分类聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
        .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        // 5.3. 规格参数的嵌套聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));
        // 6. 构建结果集过滤
        searchSourceBuilder.fetchSource(new String[]{"skuId", "title", "price", "defaultImage","subTitle"}, null);

        System.out.println(searchSourceBuilder);
        return searchSourceBuilder;
    }

}
