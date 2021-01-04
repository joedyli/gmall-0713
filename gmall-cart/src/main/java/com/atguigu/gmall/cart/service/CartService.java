package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    private static final String KEY_PREFIX = "cart:info:";

    public void addCart(Cart cart) {
        String userId = getUserId();

        // 组装外层的key
        String key = KEY_PREFIX + userId;
        // 根据外层的key，获取内存的map。即该用户的所有购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 判断该用户的购物车中是否包含该条商品
        BigDecimal count = cart.getCount();
        String skuId = cart.getSkuId().toString();
        if (hashOps.hasKey(skuId)) {
            // 包含，则更新数量
            String json = hashOps.get(skuId).toString();
            cart = JSON.parseObject(json, Cart.class);
            cart.setCount(cart.getCount().add(count));
            // 更新到redis
            this.cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
        } else {
            // 不包含，则新增一条记录
            cart.setUserId(userId);
            cart.setCheck(true);
            // 查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null){
                throw new CartException("没有对应的商品");
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());

            // 查询销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponseVo =
                    this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValues = saleAttrsResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValues));

            // 查询营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            // 查询库存信息
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 保存到redis和mysql
            this.cartMapper.insert(cart);
        }
        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    private String getUserId() {
        // 获取登录状态
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = userInfo.getUserKey();
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Long skuId) {

        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (!hashOps.hasKey(skuId.toString())) {
            throw new CartException("没有对应的购物车记录！");
        }

        String json = hashOps.get(skuId.toString()).toString();
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, Cart.class);
        }

        throw new CartException("没有对应的购物车记录！");
    }
}
