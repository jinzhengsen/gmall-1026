package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.vo.AttrGroupEntityVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

}