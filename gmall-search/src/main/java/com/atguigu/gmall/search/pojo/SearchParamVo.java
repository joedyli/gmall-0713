package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * ?keyword=手机&brandId=1,2&categoryId=225&props=4:8G-12G&props=5:128G-256G
 *  &priceFrom=1000&priceTo=3000&store=true&sort=1&pageNum=2
 */
@Data
public class SearchParamVo {

    // 搜索关键字
    private String keyword;
    // 品牌id
    private List<Long> brandId;
    // 分类id过滤条件
    private List<Long> categoryId;
    // 规格参数的过滤条件: ["4:8G-12G", "5:128G-256G"]
    private List<String> props;

    // 价格区间过滤
    private Double priceFrom;
    private Double priceTo;

    // 是否有货过滤
    private Boolean store;

    // 排序：0-综合排序 1-价格降序 2-价格升序 3-销量的降序 4-新品降序
    private Integer sort;

    // 分页参数
    private Integer pageNum = 1;
    private final Integer pageSize = 20;
}
