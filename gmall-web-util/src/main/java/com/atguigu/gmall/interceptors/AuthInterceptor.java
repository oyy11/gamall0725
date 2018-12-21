package com.atguigu.gmall.interceptors;

import com.atguigu.gmall.annotations.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断当前请求方法的拦截类型
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequire methodAnnotation = hm.getMethodAnnotation(LoginRequire.class);

        if (null == methodAnnotation) {
            //则不需要验证
            return true;
        }
        //是否必须验证通过
        boolean neededSuccess = methodAnnotation.isNeededSuccess();

        //获得用户的token
        String token = "";
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        String newToken = request.getParameter("newToken");

        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        //StringUtils.isBlank(token)表示用户没有登录过


        if (StringUtils.isNotBlank(token)) {

            //验证token，http工具
            //如果是通过负载均衡，那么ip是
            String ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isBlank(ip)) {
                //就说明不是通过负载均衡
                ip = request.getRemoteAddr();
                if (StringUtils.isBlank(ip)) {
                    ip = "127.0.0.1";
                }
            }

            //验证token
            String doGet = HttpClientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token + "&currentIp=" + ip);

            if (doGet.equals("success")) {
                //刷新用户cookie中的token
                CookieUtil.setCookie(request,response,"oldToken",token,60*30,true);

                //
                Map gmall0725 = JwtUtil.decode("gmall0725", token, ip);
                request.setAttribute("userId",gmall0725.get("userId"));
                request.setAttribute("nickName",gmall0725.get("nickName"));
                return  true;
            }

//            //验证不通过
//            if (neededSuccess == true){
//                String returnUrl = request.getRequestURL().toString();
//                //必须验证通过
//                response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl=" + returnUrl);
//                return false;
//            }else {
//                return true;
//            }
        }

        //token为空
        if (neededSuccess == true) {
            String returnUrl = request.getRequestURL().toString();
            //必须验证通过
            response.sendRedirect("http://passport.gmall.com:8085/index?returnUrl=" + returnUrl);
            return false;
        }

        //token为空且不需要登录

        return true;

    }
}
