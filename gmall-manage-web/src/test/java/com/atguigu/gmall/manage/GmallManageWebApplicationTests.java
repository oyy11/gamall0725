package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void contextLoads() throws IOException, MyException {
       String path = GmallManageWebApplicationTests.class.getClassLoader().getResource("tracker.conf").getPath();

        System.err.println(path);

        ClientGlobal.init(path);

        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();

        StorageClient storageClient = new StorageClient(connection, null);

        String[] jpgs = storageClient.upload_appender_file("e:/oyy.jpg", "jpg", null);


        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("ipdata.properties");

        Properties p = new Properties();

        try {
            p.load(inputStream); //加载
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        System.out.println("ip:"+p.getProperty("ip"));



        String url = "http://"+p.getProperty("ip");

        for(int i = 0;i<jpgs.length;i++){
            url = url +"/"+jpgs[i];
        }
        System.err.println(url);
    }

}
