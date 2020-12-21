package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    private List<BrandEntity> brands;

    private List<CategoryEntity> categories;

    // 规格参数过滤条件：[{attrId: 4, attrName: "内存", attrValues: ["8G", "12G"]},
    // {attrId: 5, attrName: "机身存储", attrValues: ["128G", "512G"]}]
    private List<SearchResponseAttrVo> filters;

    // 分页
    private Integer pageNum;
    private Integer pageSize;
    private Long total;

    private List<Goods> goodsList;
}
