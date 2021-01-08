package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.oms.entity.OrderEntity;

/**
 * 订单
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-01-08 15:24:34
 */
public interface OrderService extends IService<OrderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId);
}

