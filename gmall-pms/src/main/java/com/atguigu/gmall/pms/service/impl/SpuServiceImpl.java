package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCid(PageParamVo paramVo, Long cid) {
        // 封装查询条件
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<SpuEntity>();
        // 如果分类id不为0，要根据分类id查，否则查全部
        if (cid!=0){
            wrapper.eq("category_id",cid);
        }
        // 如果用户输入了检索条件，根据检索条件查
        String key = paramVo.getKey();
        if (StringUtils.isNotBlank(key)){
            wrapper.and(t->t.like("name",key).or().like("id",key));
        }
        return new PageResultVo(this.page(paramVo.getPage(),wrapper));
    }

    @Resource
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService baseService;
    @Resource
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    @GlobalTransactional
    public void bigSave(SpuVo spuVo) {
        /// 1.保存spu相关
        // 1.1. 保存spu基本信息 spu_info
        Long spuId = saveSpu(spuVo);

        // 1.2. 保存spu的描述信息 spu_info_desc
        saveSpuDesc(spuVo, spuId);

        // 1.3. 保存spu的规格参数信息
        saveBaseAttr(spuVo, spuId);


        /// 2. 保存sku相关信息
        savaSkuInfo(spuVo, spuId);

        // 最后制造异常
//        int i=10/0;

        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE","item.insert",spuId);


    }

    //保存sku信息
    @Transactional
    void savaSkuInfo(SpuVo spuVo, Long spuId) {
        List<SkuVo> skuVos = spuVo.getSkus();
        //判断skus是否为空，如果为空直接返回
        if (CollectionUtils.isEmpty(skuVos)){
            return;
        }

        skuVos.forEach(skuVo -> {
            // 2.1. 保存sku基本信息
//            SkuEntity skuEntity = new SkuEntity();
//            BeanUtils.copyProperties(skuVo,skuEntity);
            // 品牌和分类的id需要从spuInfo中获取
            skuVo.setSpuId(spuId);
            skuVo.setCategoryId(spuVo.getCategoryId());
            skuVo.setBrandId(spuVo.getBrandId());
//            skuEntity.setBrandId(skuVo.getBrandId());
//            skuEntity.setCategoryId(skuVo.getCategoryId());
            // 获取图片列表
            List<String> images=skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)){
                // 设置第一张图片作为默认图片
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage())?skuVo.getDefaultImage():images.get(0));
            }
            this.skuMapper.insert(skuVo);
            // 获取skuId
            Long skuId = skuVo.getId();

            // 2.2. 保存sku图片信息
            if (!CollectionUtils.isEmpty(images)){
                String defaultImage = images.get(0);
                List<SkuImagesEntity> skuImagesEntities= images.stream().map(image->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setDefaultStatus(org.apache.commons.lang.StringUtils.equals(defaultImage,image)?1:0);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setSort(0);
                    skuImagesEntity.setUrl(image);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                this.skuImagesService.saveBatch(skuImagesEntities);
            }

            // 2.3. 保存sku的规格参数（销售属性）
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            saleAttrs.forEach(saleAttr->{
                // 设置属性名，需要根据id查询AttrEntity
                saleAttr.setSort(0);
                saleAttr.setSkuId(skuId);
            });
            this.skuAttrValueService.saveBatch(saleAttrs);

            // 3. 保存营销相关信息，需要远程调用gmall-sms
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saveSkuSaleInfo(skuSaleVo);

        });
    }

    @Transactional
    void saveBaseAttr(SpuVo spuVo, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(spuAttrValueVo -> {
                spuAttrValueVo.setSpuId(spuId);
                spuAttrValueVo.setSort(0);
                return spuAttrValueVo;
            }).collect(Collectors.toList());
            this.baseService.saveBatch(spuAttrValueEntities);
        }
    }

    //保存spu的描述信息
    @Transactional
    void saveSpuDesc(SpuVo spuVo, Long spuId) {
        SpuDescEntity spuDescEntity = new SpuDescEntity();
        // 注意：spu_info_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
        spuDescEntity.setSpuId(spuId);
        // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
        spuDescEntity.setDecript(org.apache.commons.lang3.StringUtils.join(spuVo.getSpuImages(),","));
        this.spuDescMapper.insert(spuDescEntity);
    }

    //保存spu基本信息
    @Transactional
    Long saveSpu(SpuVo spuVo) {
        spuVo.setPublishStatus(1);// 默认是已上架
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        // 新增时，更新时间和创建时间一致
        this.save(spuVo);
        return spuVo.getId();
    }

}