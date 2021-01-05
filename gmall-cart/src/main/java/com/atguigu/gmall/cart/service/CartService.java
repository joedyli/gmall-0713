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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        String userId = getUserId();

        // 组装外层的key
        String key = KEY_PREFIX + userId;
        // 根据外层的key，获取内层的map。即该用户的所有购物车
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
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, skuId, cart);
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
            this.cartAsyncService.insertCart(cart);

            // 给购物车对应商品添加实时价格缓存
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());
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

    @Async
    public String executor1() throws InterruptedException {
        System.out.println("这是一个executor1方法，开始执行");
        TimeUnit.SECONDS.sleep(5);
        int i = 1/0;
        System.out.println("这是一个executor1方法，结束执行。。。。。");
        return "hello executor1";
    }

    @Async
    public ListenableFuture<String> executor2(){
        try {
            System.out.println("这是一个executor2方法，开始执行");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("这是一个executor2方法，结束执行。。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return AsyncResult.forValue("hello executor2");
    }

    public List<Cart> queryCarts() {

        UserInfo userInfo = LoginInterceptor.getUserInfo();

        // 1.先查未登录的购物车
        String userKey = userInfo.getUserKey();
        String unloginKey = KEY_PREFIX + userKey;
        // 根据外层key，获取内层map（未登录的内层map）
        BoundHashOperations<String, Object, Object> unloginHashOps = this.redisTemplate.boundHashOps(unloginKey);
        // 获取内层map中所有cart集合（字符串集合）
        List<Object> unloginCartJsons = unloginHashOps.values();
        List<Cart> unloginCarts = null;
        if (!CollectionUtils.isEmpty(unloginCartJsons)){
            // 把字符串集合反序列化为Cart集合
            unloginCarts = unloginCartJsons.stream().map(cartJson -> {
                // 把每条记录String 反序列化为 cart对象
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }

        // 2.获取登录状态，未登录则直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId == null){
            return unloginCarts;
        }

        // 3.登录，则合并到登录的购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unloginCarts)){
            unloginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount(); // 未登录状态购物车数量
                if (loginHashOps.hasKey(skuId)){
                    // 如果登录状态的购物车包含该商品，则更新数量
                    String cartJson = loginHashOps.get(skuId).toString();
                    // 获取登录状态的购物车对象
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));

                    // 异步写入到mysql
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(), skuId, cart);
                } else {
                    // 如果登录状态的购物车不包含该商品，则新增一条记录
                    // 把之前的userKey更新为userId
                    cart.setUserId(userId.toString());
                    // 异步写入到mysql
                    this.cartAsyncService.insertCart(cart);
                }
                // 同步写入到redis
                loginHashOps.put(skuId, JSON.toJSONString(cart));
            });
        }

        // 4.删除未登录的购物车
        // 同步删除redis中购物车
        this.redisTemplate.delete(unloginKey);
        // 异步删除mysql中购物车
        this.cartAsyncService.deleteByUserId(userKey);

        // 5.最后查询登录状态的购物车，并返回
        List<Object> loginCartJsons = loginHashOps.values();
        if (CollectionUtils.isEmpty(loginCartJsons)){
            return null;
        }
        return loginCartJsons.stream().map(cartJson -> {
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
            cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
            return cart;
        }).collect(Collectors.toList());
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (!hashOps.hasKey(cart.getSkuId().toString())){
            throw new CartException("该用户没有对应的购物车记录");
        }

        // 用户要更新的数量
        BigDecimal count = cart.getCount();

        // 查询redis中的购物车记录
        String json = hashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(json, Cart.class);
        cart.setCount(count); // 更新购物车中的商品数量

        hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart.getSkuId().toString(), cart);
    }

    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        hashOps.delete(skuId.toString());
        this.cartAsyncService.deleteCart(userId, skuId);
    }
}
