package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;  //在es中获取数据


    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) {
        String dsl = getMyDsl(skuLsParam);

        Search build = new Search.Builder(dsl).addIndex("gmall0725").addType("SkuLsInfo").build();

        SearchResult execute = null;
        try {
            execute = jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);

        ArrayList<SkuLsInfo> skuLsInfos = new ArrayList<>();

        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo source = hit.source;

            //替换成高亮字段
            if(hit.highlight != null){ //也就是判断是通过三级分类Id还是通过搜索过来的，搜索过来的才会有高亮

                List<String> skuName = hit.highlight.get("skuName");
                String s = skuName.get(0);
                source.setSkuName(s);
            }
            skuLsInfos.add(source);
        }

//        //取聚合对象
//        MetricAggregation aggregations = execute.getAggregations();
//        //声明聚合的valueId集合
//        List<String> attrValueList = new ArrayList<>();
//        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
//        if(groupby_attr !=null){
//            List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
//            for (TermsAggregation.Entry bucket : buckets) {
//                //取聚合函数中的valueId
//                attrValueList.add(bucket.getKey());//聚合的字段值，属性值id  (达到去从)
//                Long count = bucket.getCount(); //出现的次数
//            }
//        }


        return skuLsInfos;
    }

    private String getMyDsl(SkuLsParam skuLsParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //must  name:字段名 text:要查询的关键字
        String keyword = skuLsParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            boolQueryBuilder.must(matchQueryBuilder);

            //设置搜索结果中搜索关键字是高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red;font-weight:bolder'>");
            highlightBuilder.field("skuName");
            highlightBuilder.postTags("</span>");

            searchSourceBuilder.highlight(highlightBuilder);

        }
        //filter   value:放id
        String[] valueIds = skuLsParam.getValueId();
        if (valueIds != null && valueIds.length>0){
            for (String valueId : valueIds) {

                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        SearchSourceBuilder query = searchSourceBuilder.query(boolQueryBuilder);

        //用于分页
        searchSourceBuilder.size(20);
        searchSourceBuilder.from(0);


//        //加聚合函数
//        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
//        searchSourceBuilder.aggregation(groupby_attr);

        return searchSourceBuilder.toString();
    }
}













