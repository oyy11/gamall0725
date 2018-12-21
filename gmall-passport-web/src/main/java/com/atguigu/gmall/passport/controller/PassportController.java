package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @Reference
    CartInfoService cartInfoService;


    @RequestMapping("index")
    public String index(String returnUrl, ModelMap map){
        map.put("returnUrl",returnUrl);

        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletResponse response, HttpServletRequest request){

        //验证用户的用户名和密码，返回token
       userInfo = userService.login(userInfo);
       if (userInfo == null){
           //用户名或密码错误
           return "fail";
       }else{
           Map<String, String> map = new HashMap<>();

           map.put("userId",userInfo.getId());
           map.put("nickName",userInfo.getNickName());

           //如果是通过负载均衡，那么ip是
           String ip = request.getHeader("x-forwarded-for");
           if (StringUtils.isBlank(ip)){
               //就说明不是通过负载均衡
               ip = request.getRemoteAddr();
               if (StringUtils.isBlank(ip)){
                   ip = "127.0.0.1";
               }
           }

           String token = JwtUtil.encode("gmall0725", map, ip);


           //合并购物车，将没有登录时的购物车和原先登录时的购物车合并
           String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
           if (StringUtils.isNotBlank(cartListCookie)) {
               List<CartInfo> cartInfos = JSON.parseArray(cartListCookie, CartInfo.class);
               //合并当前用户的购物车
               cartInfoService.mergCart(cartInfos, userInfo.getId());
               //合并购物车之后，清理cookie中的购物车
               CookieUtil.setCookie(request,response,"cartListCookie","",0,true);
           }

           return token;
       }


    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,HttpServletRequest request,String currentIp){

        Map gmall0725 = JwtUtil.decode("gmall0725", token, currentIp);

        if (gmall0725 == null){
            return "fail";
        }else {
            return "success";
        }

    }
}
