package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.ItemException;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.java2d.pipe.SpanIterator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1.查询sku信息
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new ItemException("该skuId对应的商品不存在!");
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;
        }, threadPoolExecutor);

        // 2.查询分类信息
        CompletableFuture<Void> catesFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsClient.queryLvl123CategoriesByCid3(skuEntity.getCatagoryId());
            List<CategoryEntity> categoryEntities = categoryResponseVo.getData();
            itemVo.setCategories(categoryEntities);
        }, threadPoolExecutor);

        // 3.查询品牌信息
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);

        // 4.查询spu信息
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        // 5.查询sku的图片列表
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> imagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResponseVo.getData();
            itemVo.setSkuImages(skuImagesEntities);
        }, threadPoolExecutor);

        // 6.查询营销信息
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);


        // 7.查询库存信息
        CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, threadPoolExecutor);

        // 8.查询spu的所有销售属性
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValueBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> attrValueVos = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(attrValueVos);
        }, threadPoolExecutor);

        // 9.当前sku的销售属性 {4: '暗夜黑', 5: '8G', 6: '128G'}
        CompletableFuture<Void> skuAttrFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, threadPoolExecutor);

        // 10.销售属性组合和skuId的映射关系
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> skuMappingResponseVo = this.pmsClient.querySaleAttrValuesMappingSkuIdBySpuId(skuEntity.getSpuId());
            String json = skuMappingResponseVo.getData();
            itemVo.setSkusJson(json);
        }, threadPoolExecutor);

        // 11.查询商品详情
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

        // 12.查询规格参数组
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupsWithAttrsAndValuesByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuId, skuEntity.getSpuId());
            List<ItemGroupVo> groupVos = groupResponseVo.getData();
            itemVo.setGroups(groupVos);
        }, threadPoolExecutor);

        CompletableFuture.allOf(catesFuture, brandFuture, spuFuture, imagesFuture, salesFuture, wareFuture,
                saleAttrsFuture, skuAttrFuture, mappingFuture, descFuture, groupFuture);

        return itemVo;
    }
}

class CompletableFutureDemo{

    public static void main(String[] args) throws IOException {
//        CompletableFuture.runAsync(() -> {
//            System.out.println("这是通过CompletableFuture的runAsync初始化了一个子任务！" + Thread.currentThread().getName());
//        });
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("这是通过CompletableFuture的supplyAsync初始化了一个子任务！" + Thread.currentThread().getName());
//            int i = 1/0;
            return "hello completableFuture!";
        });
        CompletableFuture<String> future1 = future.thenApplyAsync(t -> {
            System.out.println("==================thenApplyAsync 1====================");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果：" + t);
            return "thenApplyAsync1";
        });
        CompletableFuture<String> future2 = future.thenApplyAsync(t -> {
            System.out.println("==================thenApplyAsync 2====================");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果：" + t);
            return "thenApplyAsync2";
        });
        CompletableFuture<Void> future3 = future.thenAcceptAsync(t -> {
            System.out.println("==================thenAcceptAsync====================");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("上一个任务的返回结果：" + t);
        });
        CompletableFuture<Void> future4 = future.thenRunAsync(() -> {
            System.out.println("==================thenRunAsync====================");
            try {
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("无法获取上一个任务的返回结果，也没有自己的返回结果");
        });
//                .whenCompleteAsync((t, u) -> {
//            System.out.println("这是whenCompleteAsync执行的结果===============================");
//            System.out.println("上一个任务的返回结果t: " + t);
//            System.out.println("上一个任务的异常信息u: " + u);
//        }).exceptionally(t -> {
//            System.out.println("上一个任务的异常信息t: " + t);
//            return "exceptionally";
//        });

        CompletableFuture.anyOf(future1, future2, future3, future4).join();

        System.out.println("这是主线程。。。。。。。");
        System.in.read();
    }
}
