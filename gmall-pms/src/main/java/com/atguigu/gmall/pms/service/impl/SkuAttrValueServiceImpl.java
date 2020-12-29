package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchSkuAttrValuesByCidAndSkuId(Long cid, Long skuId) {
        // 根据分类id和search_type查询检索类型的规格参数
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("search_type", 1).eq("category_id", cid));

        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }

        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        // 根据skuId和attrIds查询出检索类型的规格参数
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrValueBySpuId(Long spuId) {
        // 查询spu下所有的sku
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return null;
        }

        // 查询所有sku对应的销售属性
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        List<SkuAttrValueEntity> skuAttrValues = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
        if (CollectionUtils.isEmpty(skuAttrValues)){
            return null;
        }

        // 要把List<SkuAttrValueEntity>处理成List<SaleAttrValueVo>
        // 把List<SkuAttrValueEntity>处理成map<attrId, List<SkuAttrValueEntity>>
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValues.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));

        // 创建一个vo集合，用于收集SaleAttrValueVo对象
        List<SaleAttrValueVo> attrValueVos = new ArrayList<>();
        // 遍历map，每一个map中的元素转化成SaleAttrValueVo对象
        map.forEach((attrId, skuAttrValueEntities) -> {
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            saleAttrValueVo.setAttrName(skuAttrValueEntities.get(0).getAttrName());
            saleAttrValueVo.setAttrValues(skuAttrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
            attrValueVos.add(saleAttrValueVo);
        });

        return attrValueVos;
    }

    @Override
    public String querySaleAttrValuesMappingSkuIdBySpuId(Long spuId) {
        List<Map<String, Object>> maps = this.attrValueMapper.querySaleAttrValuesMappingSkuIdBySpuId(spuId);
        if (CollectionUtils.isEmpty(maps)){
            return null;
        }

        // [{sku_id=1, attr_values=黑色,8G,128G}, {sku_id=2, attr_values=白色,8G,256G}]
        // 把上边这个数据模型处理成下面这种结构
        // {'黑色,8G,128G': 1, '白色,8G,256G': 2}
        Map<String, Long> attrValuesMappingSkuIdMap = maps.stream().collect(Collectors.toMap(map -> (String)map.get("attr_values"), map -> (Long) map.get("sku_id")));
        return JSON.toJSONString(attrValuesMappingSkuIdMap);
    }

}
