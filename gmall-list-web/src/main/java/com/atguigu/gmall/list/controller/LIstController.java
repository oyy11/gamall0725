package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.BaseAttrInfoService;
import com.atguigu.gmall.service.ListService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class LIstController {

    @Reference
    ListService listService;

    @Reference
    BaseAttrInfoService baseAttrInfoService;

    @RequestMapping("index")
    public String index(){

        return "index";
    }

    @RequestMapping("list.html")
    public String list(SkuLsParam skuLsParam, ModelMap map){

        List<SkuLsInfo> skuLsInfos = listService.search(skuLsParam);

        map.put("skuLsInfoList",skuLsInfos);

        //sku列表结果中包含的属性列表
        Set<String> valueIds = new HashSet<>();
        for (SkuLsInfo skuLsInfo : skuLsInfos) {
            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();
            for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                String valueId = skuLsAttrValue.getValueId();
                valueIds.add(valueId);
            }
        }
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoService.getAttrListByValueIds(valueIds);

        //根据选择的属性值id来删除属性值列表
        String[] delValueIds = skuLsParam.getValueId();
        if(delValueIds != null && delValueIds.length>0) {  //如果为null,就表示一个也没选，不走这一步

            //面包屑
            ArrayList<Crumb> crumbs = new ArrayList<>();
            for (String delValueId : delValueIds) {
                Iterator<BaseAttrInfo> iterator = baseAttrInfos.iterator();
                Crumb crumb = new Crumb();
                while (iterator.hasNext()) {  //每次遍历都与iterator（BaseAttrInfo）中比较
                BaseAttrInfo baseAttrInfo = iterator.next();
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    String valueId = baseAttrValue.getId();

                        if (delValueId.equals(valueId)){
                            String myCrumbUrl = getMyCrumbUrl(skuLsParam, delValueId);
                            crumb.setUrlParam(myCrumbUrl);
                            crumb.setValueName(baseAttrValue.getValueName());//用于面包屑显示具体属性值

                            iterator.remove();  //根据所选的属性值id来删除它所在的属性列表
                        }
                    }

                }
                crumbs.add(crumb); //所选属性不只一个
            }
            map.put("attrValueSelectedList",crumbs);
        }


        map.put("attrList",baseAttrInfos);

        //上一次的请求参数列表
        String urlParam = getMyUrlParam(skuLsParam);
        map.put("urlParam",urlParam);

        return "list";
    }


    //选出的属性制作面包屑  动态改变
    private String getMyCrumbUrl(SkuLsParam skuLsParam,String delValueId ) {
        String urlParam = "";
        // xxx=yyy&xxx=yyy&xxx=yyy
        String keyword = skuLsParam.getKeyword();
        String catalog3Id = skuLsParam.getCatalog3Id();
        String[] valueIds = skuLsParam.getValueId();

        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam ="&";
            }
            urlParam = urlParam +"catalog3Id="+catalog3Id;
        }

        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam = "&";
            }
            urlParam = urlParam +"keyword="+keyword;
        }

        if(null != valueIds){
            for (String valueId : valueIds) {
                if(!valueId.equals(delValueId)){  //当前遍历到的valueId 不等于要删除的面包屑时，将它拼到参数中

                    urlParam = urlParam + "&valueId="+valueId;
                }
            }
        }

        return urlParam;

    }


    //
    private String getMyUrlParam(SkuLsParam skuLsParam) {
        String urlParam = "";
        // xxx=yyy&xxx=yyy&xxx=yyy
        String keyword = skuLsParam.getKeyword();
        String catalog3Id = skuLsParam.getCatalog3Id();
        String[] valueIds = skuLsParam.getValueId();

        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam ="&";
            }
            urlParam = urlParam +"catalog3Id="+catalog3Id;
        }

        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam = "&";
            }
            urlParam = urlParam +"keyword="+keyword;
        }

        if(null != valueIds){
            for (String valueId : valueIds) {
                urlParam = urlParam + "&valueId="+valueId;
            }
        }

        return urlParam;

    }
}
