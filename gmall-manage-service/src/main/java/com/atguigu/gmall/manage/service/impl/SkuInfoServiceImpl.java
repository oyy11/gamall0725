package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.SkuImageMapper;
import com.atguigu.gmall.manage.mapper.SkuInfoMapper;
import com.atguigu.gmall.manage.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuInfoService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class SkuInfoServiceImpl implements SkuInfoService {

    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<SkuInfo> skuInfoListBySpu(String spuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> skuInfos = skuInfoMapper.select(skuInfo);

        return skuInfos;
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {
        skuInfoMapper.insertSelective(skuInfo);
        String skuId = skuInfo.getId();

        // 保存sku图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insertSelective(skuImage);
        }

        // 保存sku平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }

        // 保存sku的销售属性
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);

            skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }
    }

    @Override
    public SkuInfo getSkuInfoById(String skuId) {
        SkuInfo skuInfo = null;
        //查询缓存
        Jedis jedis = redisUtil.getJedis();
        String cacheJson = jedis.get("sku:" + skuId + ":info");

        System.err.println(Thread.currentThread().getName()+"号线程，进入程序");

        if (StringUtils.isBlank(cacheJson)) {

            System.err.println(Thread.currentThread().getName()+"号线程，缓存中没有数据，申请分布式缓存锁");


            //分布式缓存锁
            String OK = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);

            if (StringUtils.isNoneBlank(OK)) {

                System.err.println(Thread.currentThread().getName()+"号线程，申请成功，访问db");

                //查询缓存没有，再查询数据库
                skuInfo = getSkuByIdFromDb(skuId);
                if (skuInfo != null) {



                    //将查询到的结果，同步到缓存
                    System.err.println(Thread.currentThread().getName()+"号线程，从db中得到数据，放入缓存");
                    jedis.set("sku:" + skuId + ":info",JSON.toJSONString(skuInfo));
                    System.err.println(Thread.currentThread().getName()+"号线程，将锁释放。。。。。");

                }

            } else {

                //自旋
                return getSkuInfoById(skuId);
            }

        } else {
            System.err.println(Thread.currentThread().getName()+"号线程，成功获得缓存数据。。。。");

            skuInfo = JSON.parseObject(cacheJson, SkuInfo.class);
        }

        jedis.close();
        return skuInfo;
    }

    private SkuInfo getSkuByIdFromDb(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImages = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImages);

        return skuInfo;
    }

    @Override
    public List<SkuInfo> skuSaleAttrValueListBySpu(String spuId) {
        List<SkuInfo> skuInfos = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
        return skuInfos;
    }

    @Override
    public List<SkuInfo> getMySkuInfo(String catalog3Id) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id(catalog3Id);
        List<SkuInfo> skuInfos = skuInfoMapper.select(skuInfo);

        for (SkuInfo info : skuInfos) {
            String skuId = info.getId();
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);

            info.setSkuAttrValueList(skuAttrValues);

        }

        return skuInfos;
    }
}
