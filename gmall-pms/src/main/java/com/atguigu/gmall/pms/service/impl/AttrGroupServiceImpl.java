package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrGroupEntityVo;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;

import javax.annotation.Resource;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {
    @Resource
    private AttrMapper attrMapper;

    @Resource
    private SpuAttrValueMapper spuAttrValueMapper;

    @Resource
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * AttrGroupEntityVo类继承AttrGroupEntity类，然后再添加了一个集合属性： private List<AttrEntity> attrEntities;
     * 根据传来的分类id(catId)来查询得到attrGroupEntities集合，然后遍历attrGroupEntities集合，讲每一个attrGroupEntity属性值赋给attrGroupEntityVo，
     * 然后根据attrGroupEntity中id的值查询数据库表中"group_id"=attrGroupEntity.getId()的attrEntities集合
     * 然后将attrEntities设置到attrGroupEntityVo对象的属性中，
     * 最后得到一个List<AttrGroupEntityVo>集合作为返回值。
     * @param catId
     * @return
     */
    @Override
    public List<AttrGroupEntityVo> queryByCid(Long catId) {
        // 查询所有的分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", catId));
        // 查询出每组下的规格参数
        return attrGroupEntities.stream().map(attrGroupEntity ->{
            AttrGroupEntityVo attrGroupEntityVo = new AttrGroupEntityVo();
            BeanUtils.copyProperties(attrGroupEntity,attrGroupEntity);
            // 查询规格参数，只需查询出每个分组下的通用属性就可以了（不需要销售属性）
            List<AttrEntity> attrEntities = this.attrMapper
                    .selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type", 1));
            attrGroupEntityVo.setAttrEntities(attrEntities);
            return attrGroupEntityVo;
        }).collect(Collectors.toList());
    }

   /* @Override
    public List<ItemGroupVo> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId) {
        //根据分类id查询出分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrGroupEntities)){
            return null;
        }

        //遍历分组查询组下的规格参数
        List<ItemGroupVo> itemGroupVoList = attrGroupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setId(attrGroupEntity.getId());
            itemGroupVo.setName(attrGroupEntity.getName());

            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (!CollectionUtils.isEmpty(attrEntities)) {
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                List<AttrValueVo> attrs = new ArrayList<>();
                //查询出基本类型规格参数
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().in("attr_id", attrIds));
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                    attrs.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                //查询出销售类型规格参数
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().in("attr_id", attrIds));
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                    attrs.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                itemGroupVo.setAttrs(attrs);

            }
            return itemGroupVo;
        }).collect(Collectors.toList());
        return itemGroupVoList;
    }*/

    @Override
    public List<ItemGroupVo> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId) {
        // 根据分类id查询出分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)){
            return null;
        }

        // 遍历分组查询出组下的规格参数
        return groupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setId(attrGroupEntity.getId());
            itemGroupVo.setName(attrGroupEntity.getName());

            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (!CollectionUtils.isEmpty(attrEntities)){
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

                List<AttrValueVo> attrs = new ArrayList<>();
                // 查询出基本类型规格参数及值
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().in("attr_id", attrIds).eq("spu_id", spuId));
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    attrs.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                // 查询出销售类型规格参数及值
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().in("attr_id", attrIds).eq("sku_id", skuId));
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrs.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                itemGroupVo.setAttrs(attrs);
            }

            return itemGroupVo;
        }).collect(Collectors.toList());
    }

}