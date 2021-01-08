package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs;
    private BigDecimal price;
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private List<ItemSaleVo> sales;
    private Integer weight;
}
