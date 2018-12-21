package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public CartInfo selectCartExists(CartInfo cartInfo) {

        //传过来的参数只需要userid和skuId
        CartInfo cartInfo1 = new CartInfo();
        cartInfo1.setUserId(cartInfo.getUserId());
        cartInfo1.setSkuId(cartInfo.getSkuId());

        return cartInfoMapper.selectOne(cartInfo1);
    }

    @Override
    public void addCart(CartInfo cartInfo) {
        cartInfoMapper.insertSelective(cartInfo);
    }

    @Override
    public void update(CartInfo cartInfoDb) {
        Example e = new Example(CartInfo.class);
        e.createCriteria().andEqualTo("userId",cartInfoDb.getUserId()).andEqualTo("skuId",cartInfoDb.getSkuId());
        //updateByExampleSelective与Example 一起使用   cartInfoDb用的部分更新，没有的部分不更新
        cartInfoMapper.updateByExampleSelective(cartInfoDb,e);
    }

    @Override
    public void flushCache(String userId) {
        Jedis jedis = redisUtil.getJedis();

        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);

        //同步缓存
        HashMap<String, String> map = new HashMap<>();
        for (CartInfo info : cartInfos) {
            map.put(info.getSkuId(),JSON.toJSONString(info));
        }

        jedis.del("user:"+userId+":cart");
        jedis.hmset("user:"+userId+":cart",map);

        jedis.close();
    }

    @Override
    public List<CartInfo> getCartListFromCache(String userId) {

        Jedis jedis = redisUtil.getJedis();
        List<CartInfo> cartInfos = new ArrayList<>();

        List<String> hvals = jedis.hvals("user:" + userId + ":cart");

        if(null != hvals && hvals.size()>0){
            for (String hval : hvals) {
                CartInfo cartInfo = new CartInfo();
                cartInfo = JSON.parseObject(hval,CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }
        jedis.close();

        return cartInfos;
    }

    @Override
    public List<CartInfo> getCartListByUserId(String userId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);

        return cartInfos;
    }

    @Override
    public void mergCart(List<CartInfo> cartInfoListCookie, String userId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        //数据库中的购物车
        List<CartInfo> cartInfoListDb = cartInfoMapper.select(cartInfo);

        for (CartInfo cartInfoCookie : cartInfoListCookie) {
            boolean b = if_new_cart(cartInfoListDb, cartInfoCookie);
            if (b){
                //在数据库中新增购物车
                cartInfoCookie.setUserId(userId);
                //向数据库中插入购物车
                cartInfoMapper.insertSelective(cartInfoCookie);
            }else{
                //更新购物车
                for (CartInfo info : cartInfoListDb) {
                    if (info.getSkuId().equals(cartInfoCookie.getSkuId())){
                        info.setSkuNum(info.getSkuNum()+cartInfoCookie.getSkuNum());
                        info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        cartInfoMapper.updateByPrimaryKeySelective(info);
                    }
                }
            }
        }

        //刷新缓存
        flushCache(userId);

    }

    private boolean if_new_cart(List<CartInfo> cartInfoListDb,CartInfo cartInfoCookie) {

        boolean b = true;

        for (CartInfo cartInfo : cartInfoListDb) {
            if (cartInfo.getSkuId().equals(cartInfoCookie.getSkuId())){
                b = false;
            }
        }
        return b ;
    }
}











