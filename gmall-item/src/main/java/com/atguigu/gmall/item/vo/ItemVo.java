package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 一级 二级 三级分类
    private List<CategoryEntity> categories;

    // 品牌信息
    private Long brandId;
    private String brandName;

    // spu信息
    private Long spuId;
    private String spuName;

    // sku相关信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;

    // 图片列表
    private List<SkuImagesEntity> skuImages;

    // 营销信息
    private List<ItemSaleVo> sales;

    // 是否有货
    private Boolean store = false;

    // 销售属性
    // [{attrId: 4, attrName: 颜色, attrValues: ['暗夜黑', '白天白']},
    // {attrId: 5, attrName: 内存, attrValues: ['8G', '12G']},
    // {attrId: 6, attrName: 存储, attrValues: ['128G', '256G']}]
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性
    // {4: '暗夜黑', 5: '8G', 6: '128G'}
    private Map<Long, String> saleAttr;

    // 销售属性组合和skuId的映射关系
    // {'白色,8G,128G': 100, '白色,8G,256G': 101}
    private String skusJson;

    // 商品详情
    private List<String> spuImages;

    // 规格参数组
    private List<ItemGroupVo> groups;
}
