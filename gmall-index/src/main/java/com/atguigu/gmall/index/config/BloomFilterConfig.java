package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.google.common.hash.BloomFilter;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Bean
    public RBloomFilter<String> bloomFilter(){
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("index:bloom:filter");
        bloomFilter.tryInit(30, 0.01);
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCatgoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        categoryEntities.forEach(categoryEntity -> {
            bloomFilter.add(KEY_PREFIX + "[" + categoryEntity.getId() + "]");
        });
        return bloomFilter;
    }
}
