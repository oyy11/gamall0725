package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequire;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.PaymentWay;
import com.atguigu.gmall.service.CartInfoService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    CartInfoService cartInfoService;

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request, ModelMap map, OrderInfo orderInfo,String tradeCode,String addressId){
        String userId = (String) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");

        boolean b = orderService.checkTradeCode(userId,tradeCode);

        UserAddress userAddress = userService.getAddressListById(addressId);
        if (b){
            //需要被删除的购物车集合
            List<String> delList = new ArrayList<>();


            //生成订单和订单详情，数据库
            List<CartInfo> cartInfos = cartInfoService.getCartListByUserId(userId);
            OrderInfo orderInfoForDb = new OrderInfo();
            //外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = sdf.format(new Date());
            orderInfoForDb.setOutTradeNo("atguigugmall"+System.currentTimeMillis()+format);
            orderInfoForDb.setOrderStatus("订单已提交");
//            orderInfoForDb.setTrackingNo("");
            orderInfoForDb.setProcessStatus("订单已提交");
            Calendar instance = Calendar.getInstance();
            //订单过期时间
            instance.add(Calendar.DATE,1);
            orderInfoForDb.setExpireTime(instance.getTime());
            orderInfoForDb.setConsigneeTel(userAddress.getPhoneNum());
            orderInfoForDb.setConsignee(userAddress.getConsignee());
            orderInfoForDb.setCreateTime(new Date());
            orderInfoForDb.setDeliveryAddress(userAddress.getUserAddress());
            orderInfoForDb.setOrderComment("硅谷订单");
            orderInfoForDb.setTotalAmount(getCartSum(cartInfos));
            orderInfoForDb.setPaymentWay(PaymentWay.ONLINE);
            orderInfoForDb.setUserId(userId);


            List<OrderDetail> orderDetails = new ArrayList<>();
            for (CartInfo cartInfo : cartInfos) {

                if (cartInfo.getIsChecked().equals("1")){
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setImgUrl(cartInfo.getImgUrl());
                    orderDetail.setSkuNum(cartInfo.getSkuNum());
                    orderDetail.setOrderPrice(cartInfo.getCartPrice());
                    orderDetail.setSkuId(cartInfo.getSkuId());
                    orderDetail.setSkuName(cartInfo.getSkuName());

                    orderDetails.add(orderDetail);

                    delList.add(cartInfo.getId());
                }
            }
            orderInfoForDb.setOrderDetailList(orderDetails);

            orderService.saveOrder(orderInfoForDb);
//            //删除购物车
//            orderService.deleteCheckedCart(delList);

            //刷新缓存
            cartInfoService.flushCache(userId);


            //重定向到支付页面

            return "redirect:http://payment.gmall.com:8090/index?outTradeNo="+orderInfoForDb.getOutTradeNo()+"&totalAmount="+getCartSum(cartInfos);
        }else {
            return "tradeFail";
        }



    }

    @LoginRequire(isNeededSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap map){

        String userId = (String) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");
        List<UserAddress> addressListByUserId = userService.getAddressListByUserId(userId);

        List<CartInfo> cartListByUserId = cartInfoService.getCartListByUserId(userId);
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartInfo cartInfo : cartListByUserId) {

            if (cartInfo.getIsChecked().equals("1")){
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());

                orderDetails.add(orderDetail);
            }
        }

        map.put("nickName",nickName);
        map.put("userAddressList",addressListByUserId);
        map.put("orderDetailList",orderDetails);
        map.put("totalAmount",getCartSum(cartListByUserId));
// 生成交易码
        String tradeCode = UUID.randomUUID().toString();
        orderService.putTradeCode(tradeCode,userId);
        map.put("tradeCode",tradeCode);
        return "trade";

    }

    private BigDecimal getCartSum(List<CartInfo> cartInfos) {

        BigDecimal sum = new BigDecimal("0");
        // 计算购物车中被选中商品的总价格
        for (CartInfo cartInfo : cartInfos) {
            if(cartInfo.getIsChecked().equals("1")){
                sum = sum.add(cartInfo.getCartPrice());
            }
        }

        return sum;
    }

}
