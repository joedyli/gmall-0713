package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVo {

    private Long skuId;
    private Integer count;
    private Boolean lock; // 响应时需要有锁定状态
    private Long wareSkuId; // 记录锁定成功的库存id，以方便将来解锁对应的库存
}
