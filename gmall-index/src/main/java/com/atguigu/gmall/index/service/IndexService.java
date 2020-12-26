package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.tools.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.security.Key;
import java.sql.Time;
import java.util.Arrays;
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

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    public List<CategoryEntity> queryLvl1CategoriesByPid(){
        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.queryCatgoriesByPid(0l);
        // ss
        // cha
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
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();

        String number = this.redisTemplate.opsForValue().get("number");
        if (StringUtils.isBlank(number)){
            return;
        }
        int num = Integer.parseInt(number);
        this.redisTemplate.opsForValue().set("number", String.valueOf(++num));

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        lock.unlock();
    }

    public void testLock3() {
        // 获取锁
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.distributedLock.tryLock("lock", uuid, 30);
        if (flag){
            String number = this.redisTemplate.opsForValue().get("number");
            if (StringUtils.isBlank(number)){
                return;
            }
            int num = Integer.parseInt(number);
            this.redisTemplate.opsForValue().set("number", String.valueOf(++num));

            //this.testSubLock(uuid);
            try {
                TimeUnit.SECONDS.sleep(90);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.distributedLock.unlock("lock", uuid);
        }
    }

    public void testSubLock(String uuid){
        this.distributedLock.tryLock("lock", uuid, 30);

        System.out.println("测试可重入锁");

        this.distributedLock.unlock("lock", uuid);
    }

    public void testLock2() {
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
            String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', 'lock') else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
//            if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//            }
        }

    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);
        System.out.println("=================");
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);
        System.out.println("===============================");
    }

    public void testLatch() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testCountdown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");

        latch.countDown();
    }
}
