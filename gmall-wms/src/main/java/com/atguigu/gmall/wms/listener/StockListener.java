package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "stock:lock:";

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_UNLOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlock(String orderToken, Message message, Channel channel) throws IOException {

        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        // 获取redis中缓存的锁定库存信息
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isBlank(json)){ // 如果缓存的锁定库存信息为空，直接消费掉消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        // 反序列化锁定库存信息的集合
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
        if (CollectionUtils.isEmpty(skuLockVos)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }
        // 遍历集合解锁库存信息
        skuLockVos.forEach(lockVo -> {
            this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
        });

        // 删除锁定库存的缓存
        this.redisTemplate.delete(KEY_PREFIX + orderToken);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_MINUS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minus(String orderToken, Message message, Channel channel) throws IOException {

        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        // 获取redis中缓存的锁定库存信息
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isBlank(json)){ // 如果缓存的锁定库存信息为空，直接消费掉消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        // 反序列化锁定库存信息的集合
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
        if (CollectionUtils.isEmpty(skuLockVos)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }
        // 遍历集合减库存信息
        skuLockVos.forEach(lockVo -> {
            this.wareSkuMapper.minus(lockVo.getWareSkuId(), lockVo.getCount());
        });

        // 删除锁定库存的缓存
        this.redisTemplate.delete(KEY_PREFIX + orderToken);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
