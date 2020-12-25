package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;

@SpringBootTest
class GmallIndexApplicationTests {


//    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("username", "柳岩");
        System.out.println(this.redisTemplate.opsForValue().get("username"));
    }

}
