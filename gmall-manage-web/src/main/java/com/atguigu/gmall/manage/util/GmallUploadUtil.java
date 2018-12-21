package com.atguigu.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GmallUploadUtil {

    public static String imgUrl(MultipartFile multipartFile){
        String path = GmallUploadUtil.class.getClassLoader().getResource("tracker.conf").getPath();

        try {
            ClientGlobal.init(path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = null;
        try {
            connection = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StorageClient storageClient = new StorageClient(connection, null);

        String originalFilename = multipartFile.getOriginalFilename();
        String[] split = originalFilename.split("\\.");
        String ext = split[split.length - 1]; //扩展名
        String[] img = new String[0];

        String[] jpgs = new String[0];
        try {
            jpgs = storageClient.upload_appender_file(multipartFile.getBytes(), ext, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        //通过配置文件，来获取服务器的ip
        InputStream inputStream = GmallUploadUtil.class.getClassLoader().getResourceAsStream("ipdata.properties");

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
        return url;
    }
}
