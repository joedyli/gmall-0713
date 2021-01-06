package com.atguigu.gmall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVo {

    private Long skuId;
    private String defaultImage;
    private String title;
    private String saleAttrs; // 销售属性：List<SkuAttrValueEntity>的json格式
    private BigDecimal price;
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private String sales; // 营销信息: List<ItemSaleVo>的json格式
    private Integer weight;
}
