package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-12-14 14:07:19
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    List<Map<String, Object>> querySaleAttrValuesMappingSkuIdBySpuId(Long spuId);
}
