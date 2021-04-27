package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;

    @Resource
    private GmallPmsClient pmsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private TemplateEngine templateEngine;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

//	1.根据skuId查询sku  V

        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new RuntimeException("该商品不存在!");
            }
            itemVo.setSkuId(skuEntity.getId());
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setWeight(skuEntity.getWeight());
            return skuEntity;
        }, threadPoolExecutor);

//	2.根据三级分类id查询一二三级分类  V
        CompletableFuture<Void> catesFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsClient.queryLvl123CategoriesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = categoryResponseVo.getData();
            itemVo.setCategories(categoryEntities);
        }, threadPoolExecutor);

//	3.根据品牌id查询品牌   V
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },threadPoolExecutor);

//	4.根据spuId查询spu信息  V
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

//	5.根据skuId查询库存信息  V
        CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

//	6.根据skuId查询营销信息 v
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);

//	7.根据skuId查询sku的图片列表  v

        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> imagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResponseVo.getData();
            itemVo.setImages(skuImagesEntities);
        }, threadPoolExecutor);

//	8.根据spuId查询spu下所有sku的销售属性  V
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValuesBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        }, threadPoolExecutor);

//	9.根据skuId查询当前sku的销售属性：{3: '白天白', 4: '8G', 5: '512G'} v
        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {

            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, threadPoolExecutor);

//	10.根据spuId查询销售属性组合和skuId的映射关系：{'白天白,8G,256G': 10, '白天白,8G,512G': 11} v
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<String> mappingResponseVo = this.pmsClient.querySaleAttrValuesMappingSkuIdBySpuId(skuEntity.getSpuId());
            String json = mappingResponseVo.getData();
            itemVo.setSkuJsons(json);
        }, threadPoolExecutor);

//	11.根据spuId查询描述信息  v
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null && StringUtils.isNotBlank(spuDescEntity.getDecript())) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

//	12.根据分类id、spuId、skuId查询规格参数组及组下的规格参数和值 v
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVos = groupResponseVo.getData();
            itemVo.setGroups(itemGroupVos);
        }, threadPoolExecutor);

        CompletableFuture.allOf(catesFuture, brandFuture, spuFuture, wareFuture, salesFuture, imageFuture,
                saleAttrsFuture, saleAttrFuture, mappingFuture, descFuture, groupFuture).join();

        return itemVo;
    }


    //生成静态页面
    public void generatedHtml(Long skuId){
//初始化thymeleaf上下文对象
        Context context = new Context();
//获取页面静态化过程中，所需的静态模型
        ItemVo itemVo = this.loadData(skuId);
        //给页面设置数据模型
        context.setVariable("itemVo",itemVo);

        //try(){}是jdk1.8新特性中的，如果有printWriter()流会自动释放
        try(PrintWriter printWriter=new PrintWriter(new File("F:\\ShangGuigu\\IdeaWorkSpace\\gulimall\\GeneratedStaticHtml\\"+skuId+".html"))){
            this.templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void asyncExecute(Long skuId){  //采用异步执行的方式，因为用户第一次根本不需要得到该静态页面，所以采用异步执行的方式生成静态页面
        threadPoolExecutor.execute(()->{
            this.generatedHtml(skuId);
        });
    }


    public void querySaleAttrValuesBySpuIdTest(Long spuId){
        ResponseVo<List<SaleAttrValueVo>> responseVo = this.pmsClient.querySaleAttrValuesBySpuId(spuId);
        List<SaleAttrValueVo> saleAttrValueVos = responseVo.getData();
        for (int i = 0; i < saleAttrValueVos.size(); i++) {
            System.out.println(saleAttrValueVos.get(i));
        }
    }

    public void test(Long skuId){
        System.out.println(skuId);
        System.out.println(this.pmsClient);
//        ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(skuId);
        ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(skuId);
        List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
            for (int i = 0; i < skuAttrValueEntities.size(); i++) {
                System.out.println(skuAttrValueEntities.get(i));
            }
        }
    }

   /* public static void main(String[] args) {
        ItemService itemService = new ItemService();
//        itemService.test(100L);
        itemService.querySaleAttrValuesBySpuIdTest(52L);
    }*/

   /* public static void main(String[] args) throws IOException {
        CompletableFuture<String> aFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("这是通过CompletableFuture初始化一个子任务。。。");
//            int i = 1/0;
            return "hello CompletableFuture";
        });
        CompletableFuture<String> bFuture = aFuture.thenApplyAsync(t -> {
            System.out.println("=================thenApplyAsync================");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果t: " + t);
            return "hello thenApplyAsync";
        });
        CompletableFuture<Void> cFuture = aFuture.thenAcceptAsync(t -> {
            System.out.println("=================thenAcceptAsync================");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果t: " + t);
        });
        CompletableFuture<Void> dFuture = aFuture.thenRunAsync(() -> {
            System.out.println("=================thenRunAsync================");
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("不获取参数，也没有返回结果集");
        });
//        CompletableFuture.allOf(bFuture,cFuture,dFuture).join();
        CompletableFuture.anyOf(bFuture,cFuture,dFuture).join();

          *//*      .whenCompleteAsync((t,u)->{
            System.out.println("=================whenCompleteAsync================");
            System.out.println("上一个任务的返回结果t: " + t);
            System.out.println("上一个任务的异常信息u: " + u);
        }).exceptionally(t->{
            System.out.println("=================exceptionally================");
            System.out.println("上一个任务的异常信息t: " + t);
            return "hello exceptionally";
        });*//*

        System.out.println("这是main方法。。。。。。");
        System.in.read();
    }*/
}
