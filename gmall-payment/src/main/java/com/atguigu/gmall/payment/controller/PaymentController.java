package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.pojo.PaymentInfoEntity;
import com.atguigu.gmall.payment.pojo.UserInfo;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

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

    @GetMapping("alipay.html")
    @ResponseBody // 本质：以其他视图的形式展示方法的返回结果集
    public String toAlipay(@RequestParam("orderToken")String orderToken){
        // 根据订单编号查询订单
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity == null || orderEntity.getStatus() != 0) {
            throw new OrderException("这个订单不属于您，或者订单状态异常");
        }

        try {

            // 调用支付宝的接口，跳转到支付宝支付页面
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);
            payVo.setTotal_amount("0.01");// 建议直接写死：0.01
            payVo.setSubject("谷粒商城支付平台");

            // 生成对账记录，返回对账记录id
            String payId = this.paymentService.savePayment(payVo);
            // 把对账记录的id，放入passback_params参数，将来支付成功之后，在异步回调时，会原路返回
            payVo.setPassback_params(payId);

            return this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("pay/success")
    public String paySuccess(){
        // TODO: 获取订单编号，根据订单编号查询订单
        return "paySuccess";
    }

    @PostMapping("pay/ok")
    @ResponseBody
    public Object payOk(PayAsyncVo payAsyncVo){
        System.out.println("异步回调接口：XXXXXXXXXXXXXXXXXXXXXX");
        // 1.验签
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        // 2.校验业务参数：平台appid 订单out_trade_no 金额:totalAmount
        // 获取响应信息中的这些参数
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        // 平台参数
        String cur_app_id = alipayTemplate.getApp_id();
        // 获取原样返回的参数
        String payId = payAsyncVo.getPassback_params();
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryPayInfoById(payId);
        if (paymentInfoEntity == null || !StringUtils.equals(app_id, cur_app_id)
                || !StringUtils.equals(out_trade_no, paymentInfoEntity.getOutTradeNo())
                || new BigDecimal(total_amount).compareTo(paymentInfoEntity.getTotalAmount()) != 0
        ) {
            return "failure";
        }

        // 3.校验支付状态：TRADE_SUCCESS
        String trade_status = payAsyncVo.getTrade_status();
        if (!StringUtils.equals(trade_status, "TRADE_SUCCESS")){
            return "failure";
        }

        // 4.修改对账表
        if (this.paymentService.updatePayInfo(payAsyncVo)) {
            // 5.修改订单状态
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.success", payAsyncVo.getOut_trade_no());
        }

        // 6.返回success，失败返回failure
        return "success";
    }

    @GetMapping("seckill/{skuId}")
    public ResponseVo seckill(@PathVariable("skuId")Long skuId){

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        RLock fairLock = this.redissonClient.getFairLock("seckill:lock:" + skuId);
        fairLock.lock();
        // 查询秒杀库存
        String stockString = this.redisTemplate.opsForValue().get("seckill:stock:" + skuId);
        if (StringUtils.isBlank(stockString)){
            return ResponseVo.fail("手慢了，秒杀结束！");
        }

        this.redisTemplate.opsForValue().decrement("seckill:stock:" + skuId);

        Map<String, Object> map = new HashMap<>();
        String orderToken = IdWorker.getTimeId();
        map.put("userId", userId);
        map.put("skuId", skuId);
        map.put("count", 1);
        map.put("orderToken", IdWorker.getTimeId());
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "seckill.success", map);

        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("seckill:countdown:" + orderToken);
        countDownLatch.trySetCount(1);

        fairLock.unlock();
        return ResponseVo.ok(orderToken);
    }

    @RequestMapping("seckill/order/{orderToken}")
    public String queryOrder(@PathVariable("orderToken")String orderToken) throws InterruptedException {

        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("seckill:countdown:" + orderToken);
        countDownLatch.await();

        // TODO: 调用oms接口查看秒杀订单（用户id orderToken）

        return "order";
    }
}
