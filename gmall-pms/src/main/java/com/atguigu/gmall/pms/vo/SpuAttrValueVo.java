package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SpuAttrValueVo extends SpuAttrValueEntity {
    public void setValueSelected(List<Object> valueSelected){
        // 如果接受的集合为空，则不设置
        if (CollectionUtils.isEmpty(valueSelected)){
            return;
        }
        this.setAttrValue(StringUtils.join(valueSelected,","));
    }
}
