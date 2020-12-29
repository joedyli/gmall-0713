package com.atguigu.gmall.index;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

@SpringBootTest
class GmallIndexApplicationTests {


//    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {

        RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter("bloomFilter");
        bloomFilter.tryInit(10, 0.03);
        bloomFilter.add("1");
        bloomFilter.add("2");
        bloomFilter.add("3");
        bloomFilter.add("4");
        bloomFilter.add("5");
        System.out.println(bloomFilter.contains("1"));
        System.out.println(bloomFilter.contains("3"));
        System.out.println(bloomFilter.contains("5"));
        System.out.println(bloomFilter.contains("6"));
        System.out.println(bloomFilter.contains("7"));
        System.out.println(bloomFilter.contains("8"));
        System.out.println(bloomFilter.contains("9"));
        System.out.println(bloomFilter.contains("10"));
        System.out.println(bloomFilter.contains("11"));
        System.out.println(bloomFilter.contains("12"));
        System.out.println(bloomFilter.contains("13"));
        System.out.println(bloomFilter.contains("14"));
        System.out.println(bloomFilter.contains("15"));
        System.out.println(bloomFilter.contains("16"));
        System.out.println(bloomFilter.contains("17"));
        System.out.println(bloomFilter.contains("18"));
        System.out.println(bloomFilter.contains("19"));
        System.out.println(bloomFilter.contains("20"));

//        this.redisTemplate.opsForValue().set("username", "柳岩");
//        System.out.println(this.redisTemplate.opsForValue().get("username"));
    }

    public static void main(String[] args) {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10, 0.3);
        bloomFilter.put("1");
        bloomFilter.put("2");
        bloomFilter.put("3");
        bloomFilter.put("4");
        bloomFilter.put("5");
        System.out.println(bloomFilter.mightContain("1"));
        System.out.println(bloomFilter.mightContain("3"));
        System.out.println(bloomFilter.mightContain("5"));
        System.out.println(bloomFilter.mightContain("6"));
        System.out.println(bloomFilter.mightContain("7"));
        System.out.println(bloomFilter.mightContain("8"));
        System.out.println(bloomFilter.mightContain("9"));
        System.out.println(bloomFilter.mightContain("10"));
        System.out.println(bloomFilter.mightContain("11"));
        System.out.println(bloomFilter.mightContain("12"));
        System.out.println(bloomFilter.mightContain("13"));
        System.out.println(bloomFilter.mightContain("14"));
        System.out.println(bloomFilter.mightContain("15"));
        System.out.println(bloomFilter.mightContain("16"));
        System.out.println(bloomFilter.mightContain("17"));
        System.out.println(bloomFilter.mightContain("18"));
        System.out.println(bloomFilter.mightContain("19"));
        System.out.println(bloomFilter.mightContain("20"));
    }

}
