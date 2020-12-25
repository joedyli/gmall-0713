package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.security.Key;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cates:";

    public List<CategoryEntity> queryLvl1CategoriesByPid(){
        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.queryCatgoriesByPid(0l);
        return responseVo.getData();
    }

    public List<CategoryEntity> queryLvl2CategoriesWithSubByPid(Long pid) {
        // 1.先查询缓存
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json) || !StringUtils.equals("null", json)){
            return JSON.parseArray(json, CategoryEntity.class);
        }

        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubsByPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();

        // 放入缓存
        if (CollectionUtils.isEmpty(data)){
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, null, 3, TimeUnit.MINUTES);
        } else {
            // 为了防止缓存雪崩，给缓存时间添加随机值
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(data), 30 + new Random().nextInt(10), TimeUnit.DAYS);
        }
        return data;
    }

    public void testLock() {
        // 获取锁
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if (!flag){
            try {
                // 获取锁失败，自旋
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //this.redisTemplate.expire("lock", 3, TimeUnit.SECONDS);

            String number = this.redisTemplate.opsForValue().get("number");
            if (StringUtils.isBlank(number)){
                return;
            }
            int num = Integer.parseInt(number);
            this.redisTemplate.opsForValue().set("number", String.valueOf(++num));

            // 释放锁
            if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))){
                this.redisTemplate.delete("lock");
            }
        }

    }
}
