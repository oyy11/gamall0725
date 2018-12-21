package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartInfoService {
    CartInfo selectCartExists(CartInfo cartInfo);

    void addCart(CartInfo cartInfo);

    void update(CartInfo cartInfoDb);

    void flushCache(String userId);

    List<CartInfo> getCartListFromCache(String userId);

    List<CartInfo> getCartListByUserId(String userId);

    void mergCart(List<CartInfo> cartInfos, String userId);
}
