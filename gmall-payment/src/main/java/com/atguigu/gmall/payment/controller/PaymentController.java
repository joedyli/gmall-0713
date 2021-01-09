package com.atguigu.gmall.payment.controller;

import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("pay.html")
    public String pay(@RequestParam("orderToken")String orderToken, Model model){

        // 根据订单编号查询订单
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity == null || orderEntity.getStatus() != 0) {
            throw new OrderException("这个订单不属于您，或者订单状态异常");
        }

        model.addAttribute("orderEntity", orderEntity);

        return "pay";
    }
}
