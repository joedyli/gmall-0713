package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Autowired
    private GmallOmsClient omsClient;


    public OrderEntity queryOrder(String orderToken) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByUserIdAndOrderToken(orderToken, userInfo.getUserId());
        return orderEntityResponseVo.getData();
    }
}
