package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("SEARCH_INSERT_QUEUE"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key ={"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId==null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }




        //获取spu下的所有sku信息
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.list(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)) {

            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity==null){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }

            // 在sku不为空的情况下，需要查询品牌和分类
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
            CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
            // List<SkuEntity> --> List<Goods>
            this.goodsRepository.saveAll(skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();
                goods.setCreateTime(spuEntity.getCreateTime());

                // sku相关的5个字段
                goods.setSkuId(skuEntity.getId());
                goods.setDefaultImage(skuEntity.getDefaultImage());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                goods.setPrice(skuEntity.getPrice().doubleValue());

                //查询库存信息
                ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)){
                    //对仓库的所有销量求和
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce(Long::sum).get());
                    // 判断所有仓库的库存是否有任何一个有货（stock - stock_locked）
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
                }

                // 品牌
                if (brandEntity!=null){
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }

                //分类
                if (categoryEntity!=null){
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                // 检索类型的规格参数及值
                List<SearchAttrValueVo> searchAttrs=new ArrayList<>();

                ResponseVo<List<SpuAttrValueEntity>> baseSearchAttrResponseVo = this.pmsClient.querySearchAttrValueBySpuId(categoryEntity.getId(), skuEntity.getSpuId());
                List<SpuAttrValueEntity> baseSearchAttrs = baseSearchAttrResponseVo.getData();
                if (!CollectionUtils.isEmpty(baseSearchAttrs)){
                    // 把List<SpuAttrValueEntity> --> List<SearchAttrValueVo>，并把转化后的集合放入searchAttrs
                    searchAttrs.addAll( baseSearchAttrs.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }

                ResponseVo<List<SkuAttrValueEntity>> saleSearchAttrResponseVo = this.pmsClient.querySearchAttrValueBySkuId(categoryEntity.getId(), skuEntity.getId());
                List<SkuAttrValueEntity> saleSearchAttrs = saleSearchAttrResponseVo.getData();
                if (!CollectionUtils.isEmpty(saleSearchAttrs)){
                    // 把List<SkuAttrValueEntity> --> List<SearchAttrValueVo>，并把转化后的集合放入searchAttrs
                    searchAttrs.addAll(saleSearchAttrs.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                goods.setSearchAttrs(searchAttrs);
                return goods;
            }).collect(Collectors.toList()));
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }



}
