package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.SkuInfoService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuInfoService skuInfoService;

    @Reference
    CartInfoService cartInfoService;


    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("checkCart")
    public String checkCart(String skuId,String isCheckedFlag,ModelMap map, HttpServletRequest request,HttpServletResponse response){
        //有用户登录 没用户登录 修改不一样

        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfos = new ArrayList<>();

        if (StringUtils.isNotBlank(userId)){
            //有用户登录
            //改数据库中的
            cartInfos = cartInfoService.getCartListByUserId(userId);
        }else{
            //没有登录，从cookie中取出购物车集合
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
        }

        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getSkuId().equals(skuId)){
                //通过skuId,修改商品的选中状态
                cartInfo.setIsChecked(isCheckedFlag);
                //
                if(StringUtils.isNotBlank(userId)) {
                    //更新数据库购物车
                    cartInfoService.update(cartInfo);
                }
            }

        }

        //修改购物车状态
        if(StringUtils.isNotBlank(userId)){

            //刷新缓存
            cartInfoService.flushCache(userId);
        }else{

            //修改cookie
            CookieUtil.setCookie(request,response,"cartListCookie",JSON.toJSONString(cartInfos),60*60*24,true);

        }


        //刷新购物车列表
        map.put("cartList",cartInfos);
        map.put("totalPrice",getCartSum(cartInfos));
        return "cartListInner";
    }


    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap map){

        //判断用户是否登录
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfos = new ArrayList<>();

        if(StringUtils.isBlank(userId)){
            //取cookie中的数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)){
                cartInfos = JSON.parseArray(cartListCookie,CartInfo.class);
            }
        }else {
            //从缓存中取
           cartInfos = cartInfoService.getCartListFromCache(userId);
        }

        map.put("cartList",cartInfos);

        BigDecimal totalPrice = getCartSum(cartInfos);
        map.put("totalPrice",totalPrice);

        return "cartList";
    }

    private BigDecimal getCartSum(List<CartInfo> cartInfos) {

        //购物车被选中商品的总价
        BigDecimal sum = new BigDecimal(0);
        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")){
                sum = sum.add(cartInfo.getCartPrice());
            }
        }

        return sum;
    }

    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("addToCart")
    public ModelAndView  addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, int num) {

        //通过商品id，获取该商品的信息
        SkuInfo skuInfo = skuInfoService.getSkuInfoById(skuId);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setCartPrice(skuInfo.getPrice());
        cartInfo.setSkuNum(num);
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setIsChecked("1");//设置是否被选中
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setSkuId(skuInfo.getId());
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

        //用户id,做单点登录时再处理

        List<CartInfo> cartInfos = new ArrayList<>();

        String userId = (String) request.getAttribute("userId");
        //判断用户是否登录
        if (StringUtils.isNotBlank(userId)) {
            //用户已登录
            cartInfo.setUserId(userId);
            //从数据库中获取该用户的购物车信息
            CartInfo cartInfoDb = cartInfoService.selectCartExists(cartInfo);
            if(cartInfoDb == null){
                //没有购物车，就插入
                cartInfoService.addCart(cartInfo);

            }else{

                //有 就更新
                cartInfoDb.setSkuNum(cartInfoDb.getSkuNum() + num);
                cartInfoDb.setCartPrice(cartInfoDb.getSkuPrice().multiply(new BigDecimal(cartInfoDb.getSkuNum())));


                cartInfoService.update(cartInfoDb);
            }

            //刷新缓存
            cartInfoService.flushCache(userId);


        } else {
            //用户没有登录
            //获取Cookie, 看cookie中是否购物车中的信息
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartListCookie)) { //购物车为空
                //购物车中没有该商品，将该商品的购物车放入Cookie中
                // 将购物车集合放入cookie
                cartInfos.add(cartInfo);
            } else {
                //购物车不为空
                // cookie中的购物车集合
                cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);

                boolean b = if_new_cart(cartInfos,cartInfo);
                if(b){
                    cartInfos.add(cartInfo);
                }else{
                    for (CartInfo info : cartInfos) {
                        if(info.getSkuId().equals(cartInfo.getSkuId())){
                            info.setSkuNum(info.getSkuNum()+num);
                            info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));
                        }
                    }
                }
            }
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(cartInfos),60*60*24,true);

        }
        ModelAndView modelAndView = new ModelAndView("redirect:toSuccess");

        //重定向时，想页面上携带的数据，（格式）
        modelAndView.addObject("skuId",skuId);
        modelAndView.addObject("skuName",skuInfo.getSkuName());
        modelAndView.addObject("skuDefaultImg",skuInfo.getSkuDefaultImg());
        modelAndView.addObject("skuNum",num);

        return modelAndView;

    }

    private boolean if_new_cart(List<CartInfo> cartInfos, CartInfo cartInfo) {
        boolean b = true;

        for (CartInfo info : cartInfos) {
            if(cartInfo.getSkuId().equals(info.getSkuId())){
                b = false;;
            }
        }
        return b;
    }


    @LoginRequire(isNeededSuccess = false)
    @RequestMapping("toSuccess")
    public String toSuccess(SkuInfo skuInfo,String skuNum,ModelMap map){


        map.put("skuInfo",skuInfo);
        map.put("skuNum",skuNum);

        return "success";
    }


    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request){

        String userId = (String) request.getAttribute("userId");


        //判断用户是否存在

        if(StringUtils.isNotBlank(userId)){
            return "tradeTest";
        }else{
            return "redirect:http://passport.gmall.com:8085/index";
        }
    }
}
