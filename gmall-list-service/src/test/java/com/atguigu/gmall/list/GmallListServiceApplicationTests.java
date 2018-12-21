package com.atguigu.gmall.list;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.SkuInfoService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    @Autowired
    JestClient jestClient;

    @Reference
    SkuInfoService skuInfoService;

    @Test
    public void contextLoads() throws IOException {

        List<SkuInfo> skuInfos =  skuInfoService.getMySkuInfo("61");

        System.err.println(skuInfos);

        ArrayList<SkuLsInfo> skuLsInfos = new ArrayList<>();

        for (SkuInfo skuInfo : skuInfos) {

            SkuLsInfo skuLsInfo = new SkuLsInfo();

            BeanUtils.copyProperties(skuInfo,skuLsInfo);
            skuLsInfos.add(skuLsInfo);
        }

        //再将封装好的skuLsInfos放入到es所建立的数据库表中(在页面客户端建立的)
        for (SkuLsInfo skuLsInfo : skuLsInfos) {
            Index build = new Index.Builder(skuLsInfo).index("gmall0725").type("SkuLsInfo").id(skuLsInfo.getId()).build();
            jestClient.execute(build);
        }

        System.err.println(skuLsInfos.size());
    }


    public static String getMyDsl(){

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //must  name:字段名 text:要查询的关键字
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","硅谷");
        boolQueryBuilder.must(matchQueryBuilder);

        //value:放id
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", "39");
        boolQueryBuilder.filter(termQueryBuilder);

        SearchSourceBuilder query = searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(20);
        searchSourceBuilder.from(0);

        System.err.println(query.toString());


        return searchSourceBuilder.toString();
    }


//查询
    @Test
    public void getMySkuLsInfo() throws IOException {

        String dsl = getMyDsl();

        Search build = new Search.Builder(dsl).addIndex("gmall0725").addType("SkuLsInfo").build();

        SearchResult execute = jestClient.execute(build);

        List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);

        ArrayList<SkuLsInfo> skuLsInfos = new ArrayList<>();

        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo source = hit.source;

            skuLsInfos.add(source);
        }
        System.err.println(skuLsInfos.size());
    }

    public void search() throws IOException {
        Search build = new Search.Builder("{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"name\": \"红海战役\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "GET /movie_chn/movie/_search\n" +
                "{\n" +
                "  \"query\": {\n" +
                "    \"term\": {\n" +
                "      \"actorList.name\": \"张译\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n").addIndex("movie_chn").addType("movie").build();

        SearchResult execute = jestClient.execute(build);

        List<SearchResult.Hit<Object, Void>> hits = execute.getHits(Object.class);
        for (SearchResult.Hit<Object, Void> hit : hits) {
            System.err.println(hit);
        }
    }

}
