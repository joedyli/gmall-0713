package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-01-08 15:24:34
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
