package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

@Data
public class AttrGroupEntityVo extends AttrGroupEntity {
    private List<AttrEntity> attrEntities;
}
