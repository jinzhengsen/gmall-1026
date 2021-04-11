package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

/**
 * spu扩展对象
 * 包含：spu基本信息、spuImages图片信息、baseAttrs基本属性信息、skus信息
 */
@Data
public class SpuVo extends SpuEntity {
    // 图片信息
    private List<String> spuImages;
    // 基本属性信息
    private List<SpuAttrValueVo> baseAttrs;
    // sku信息
    private List<SkuVo> skus;
}
