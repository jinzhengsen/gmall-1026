package com.atguigu.gmall.pms.service.impl;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;

import javax.annotation.Resource;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {
    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {
        // 构造查询条件
        QueryWrapper<CategoryEntity> queryWrapper=new QueryWrapper<>();
        // 如果parentId为-1，说明用户没有传该字段，查询所有
        if (parentId!=-1){
            queryWrapper.eq("parent_Id",parentId);
        }
      /*  List<CategoryEntity> categoryEntities = categoryMapper.selectList(queryWrapper);
        return categoryEntities;*/
        return this.list(queryWrapper);
    }

    @Override
    public List<CategoryEntity> queryLvl2WithSubsByPid(Long pid) {
        return this.categoryMapper.queryLvl2WithSubsByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryLvl123CategoriesByCid3(Long cid3) {
        //查询三级分类
        CategoryEntity categoryEntity3 = this.getById(cid3);
        if (categoryEntity3==null){
            return null;
        }
        //查询二级分类
        CategoryEntity categoryEntity2 = this.getById(categoryEntity3.getParentId());
        if (categoryEntity2==null){
            return null;
        }
        //查询一级分类
        CategoryEntity categoryEntity1 = this.getById(categoryEntity2.getParentId());

        return Arrays.asList(categoryEntity1,categoryEntity2,categoryEntity3);

    }

}