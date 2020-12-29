package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @PostMapping("pms/spu/page")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCatgoriesByPid(@PathVariable("parentId")Long pid);

    @GetMapping("pms/category/parent/withsub/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubsByPid(@PathVariable("pid")Long pid);

    @GetMapping("pms/category/all/{id}")
    public ResponseVo<List<CategoryEntity>> queryLvl123CategoriesByCid3(@PathVariable("id")Long id);

    @GetMapping("pms/skuattrvalue/category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchSkuAttrValuesByCidAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("sku_id")Long skuId
    );

    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValueBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValueBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuattrvalue/mapping/spu/{spuId}")
    public ResponseVo<String> querySaleAttrValuesMappingSkuIdBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spuattrvalue/category/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchSpuAttrValuesByCidAndSpuId(
            @PathVariable("cid")Long cid,
            @RequestParam("spu_id")Long spuId
    );

    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/withattr/value/category/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupsWithAttrsAndValuesByCidAndSpuIdAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("skuId")Long skuId,
            @RequestParam("spuId")Long spuId
    );
}
