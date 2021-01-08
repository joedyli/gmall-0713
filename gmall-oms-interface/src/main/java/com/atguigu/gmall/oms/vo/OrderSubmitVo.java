package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    // 防重的唯一标识
    private String orderToken;

    // 验总价，页面会提交下单时的总价格
    private BigDecimal totalPrice;

    // 送货清单：验总价 验库存  订单详情
    private List<OrderItemVo> items;

    // 支付方式
    private Integer payType;

    // 配送方式、快递公司
    private String deliveryCompany;

    // 收货地址
    private UserAddressEntity address;

    // 积分信息
    private Integer bounds;
}
