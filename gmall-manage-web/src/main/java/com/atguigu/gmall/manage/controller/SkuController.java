package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.BaseAttrInfoService;
import com.atguigu.gmall.service.SkuInfoService;
import com.atguigu.gmall.service.SpuInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SkuController {

    @Reference
    SkuInfoService skuInfoService;

    @Reference
    BaseAttrInfoService baseAttrInfoService;

    @Reference
    SpuInfoService spuInfoService;

    @RequestMapping("saveSku")
    @ResponseBody
    public String saveSku(SkuInfo skuInfo){

        skuInfoService.saveSku(skuInfo);

        return "SUCCESS";
    }

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){

        List<SpuImage> spuImages = spuInfoService.spuImageList(spuId);

        return spuImages;
    }


    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){

        List<SpuSaleAttr> spuSaleAttrs = spuInfoService.spuSaleAttrList(spuId);

        return spuSaleAttrs;
    }


    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){

        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoService.attrInfoList(catalog3Id);

        return baseAttrInfos;
    }

    @RequestMapping("skuInfoListBySpu")
    @ResponseBody
    public List<SkuInfo> skuInfoListBySpu(String spuId){

        List<SkuInfo> skuInfos = skuInfoService.skuInfoListBySpu(spuId);

        return skuInfos;
    }
}
