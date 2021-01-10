package com.atguigu.gmall.payment.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import com.atguigu.gmall.payment.pojo.UserInfo;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    public OrderEntity queryOrder(String orderToken) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByUserIdAndOrderToken(orderToken, userInfo.getUserId());
        return orderEntityResponseVo.getData();
    }

    public String savePayment(PayVo payVo) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setOutTradeNo(payVo.getOut_trade_no());
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setPaymentType(1);
        paymentInfoEntity.setSubject(payVo.getSubject());
        paymentInfoEntity.setTotalAmount(new BigDecimal(payVo.getTotal_amount()));
        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId().toString();
    }

    public PaymentInfoEntity queryPayInfoById(String payId){
        return this.paymentInfoMapper.selectById(payId);
    }

    @Transactional
    public Boolean updatePayInfo(PayAsyncVo payAsyncVo){
        PaymentInfoEntity paymentInfoEntity = this.queryPayInfoById(payAsyncVo.getPassback_params());
        if (paymentInfoEntity.getPaymentStatus() == 1) {
            return false;
        }
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        return this.paymentInfoMapper.updateById(paymentInfoEntity) == 1;
    }
}
