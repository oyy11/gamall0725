package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuInfo;

import java.util.List;

public interface SkuInfoService {
    List<SkuInfo> skuInfoListBySpu(String spuId);

    void saveSku(SkuInfo skuInfo);


    SkuInfo getSkuInfoById(String skuId);

    List<SkuInfo> skuSaleAttrValueListBySpu(String spuId);

    List<SkuInfo> getMySkuInfo(String catalog3Id);
}
