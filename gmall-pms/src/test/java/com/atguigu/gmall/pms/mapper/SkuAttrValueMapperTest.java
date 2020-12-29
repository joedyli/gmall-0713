package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SkuAttrValueMapperTest {

    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Test
    void querySaleAttrValuesMappingSkuIdBySpuId() {
        System.out.println(this.attrValueMapper.querySaleAttrValuesMappingSkuIdBySpuId(7l));
    }
}
