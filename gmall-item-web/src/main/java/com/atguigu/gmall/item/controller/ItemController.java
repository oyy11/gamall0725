package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.SkuInfoService;
import com.atguigu.gmall.service.SpuInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    SkuInfoService skuInfoService;

    @Reference
    SpuInfoService spuInfoService;

    @RequestMapping("{skuId}.html")
    public String itemPage(@PathVariable String skuId,ModelMap map){

        SkuInfo skuInfo = skuInfoService.getSkuInfoById(skuId);

        map.put("skuInfo",skuInfo);
        //获取spuId来获得销售属性和值
        String spuId = skuInfo.getSpuId();

        //获取销售属性集合
        List<SpuSaleAttr> spuSaleAttrs = spuInfoService.spuSaleAttrListBySpuId(spuId,skuId);
        map.put("spuSaleAttrListCheckBySku",spuSaleAttrs);

        //根据spuId制作页面销售属性的hash表（隐藏的）
        //{销售属性组合：skuId}
        List<SkuInfo> skuInfos = skuInfoService.skuSaleAttrValueListBySpu(spuId);
        HashMap<String, String> saleAttrStringMap = new HashMap<>();
        for (SkuInfo info : skuInfos) {
            String skuSaleAttrValueIdsKey ="";
            List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValueIdsKey = skuSaleAttrValueIdsKey +"|"+skuSaleAttrValue.getSaleAttrValueId();

            }
            String skuIdValue = info.getId();
            saleAttrStringMap.put(skuSaleAttrValueIdsKey,skuIdValue);
        }
        String s = JSON.toJSONString(saleAttrStringMap);
        map.put("valuesSkuJson",s);

        return "item";
    }
}
