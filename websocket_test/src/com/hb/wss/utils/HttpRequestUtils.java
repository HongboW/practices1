package com.hb.wss.utils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于HttpClient的请求工具类
 * Created by Hongbo on 2017/6/20.
 */
public class HttpRequestUtils {
    private static final String UTF_8 = HTTP.UTF_8;
    /**
     * 定制连接设置
     */
    private static RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(5000).setSocketTimeout(5000).setRedirectsEnabled(true).build();

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
/*        String url = "http://10.40.2.10:7001/sxqmcb/pages/html/index.html";
        URI uri = null;*/
/*        URI uri = getURI("10.40.2.10", 7001, "/sxqmcb/pages/html/index.html");

        List<BasicNameValuePair> list = new ArrayList<>();
        list.add(new BasicNameValuePair("login_uid", "lf_system"));
        list.add(new BasicNameValuePair("login_pwd", "888888"));
        sendHttpPost(uri, list);*/
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String uri = "http://10.40.2.10:7001/sxqmcb/reports/crud";  //提交路径
        String filePath = "B://qmcb_170621192239_40W_16min/qmcb_1.xls"; //文件路径
        HttpPost httpPost = new HttpPost(uri);
        FileBody b_in = new FileBody(new File(filePath));  //import httpmime.jar;将文件转换为流对象

        StringBody serviceid = new StringBody("CheckExcelDatasService", ContentType.create("text/plain", UTF_8));
        StringBody loginname = new StringBody("lf_1", ContentType.create("text/plain", UTF_8));
        StringBody password = new StringBody("21218cca77804d2ba1922c33e0151105", ContentType.create("text/plain", UTF_8));

        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("importExcelData", b_in)
                .addPart("serviceid", serviceid)
                .addPart("loginname", loginname)
                .addPart("password", password)
                .build();    //请求实体

        reqEntity = MultipartEntityBuilder.create()
                .addTextBody("serviceid", "syslogin", ContentType.DEFAULT_TEXT)
                .addTextBody("username", "lf_1", ContentType.DEFAULT_TEXT)
                .addTextBody("passwd", "21218cca77804d2ba1922c33e0151105", ContentType.DEFAULT_TEXT)
                .build();

        httpPost.setEntity(reqEntity);  //设置实体

        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            System.out.println("请求状态：" + httpResponse.getStatusLine());
//            System.out.println("响应内容:" + httpResponse);

            Header[] header = httpResponse.getAllHeaders();
            for (Header h : header) {
                System.out.println(h);
            }

            System.err.println("Cookie:" + httpResponse.getLastHeader("Set-Cookie").getValue());
            /*
            HttpEntity resEntity = httpResponse.getEntity();    //响应实体
            System.out.println("响应实体长度：" + resEntity.getContentLength());
            System.out.println("响应实体内容：" + EntityUtils.toString(resEntity, UTF_8));
            EntityUtils.consumeQuietly(resEntity);
            */
            HttpClientUtils.closeQuietly(httpResponse);

        } catch (IOException e) {
            System.err.println("请求时异常：" + e.getMessage());
        } finally {
            HttpClientUtils.closeQuietly(httpClient);
        }
    }

    /**
     * 获取URL
     * 目前只定义HTTP协议
     * @param str_host
     * @param i_port
     * @param str_path
     * @return
     */
    public static URI getURI (String str_host, int i_port, String str_path) {
        URI uri = null;
        try {
            uri = new URIBuilder().setScheme("http").setHost(str_host).setPort(i_port).setPath(str_path).build();
        } catch (URISyntaxException e) {
            System.err.println("URL路径异常：" + e.getMessage());
        } finally {
            return uri;
        }
    }

    /**
     * 发送GET请求
     * 参数定制
     * @param str_uri
     */
    public static void sendHttpGet(String str_uri) {
        CloseableHttpClient httpClient = HttpClients.createDefault();   //定义可关闭的HttpClient
        HttpGet httpGet = new HttpGet(str_uri);
        httpGet.setConfig(requestConfig);
        String str_response = "";
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            str_response = accessText(response);
        } catch (IOException e) {
            System.err.println("GET请求异常：" + e.getMessage());
        } finally {
            try {
                httpClient.close(); //释放资源
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(str_response);
    }

    /**
     * 发送POST请求
     * @param uri 请求URL
     */
    public static void sendHttpPost(URI uri, List<BasicNameValuePair> paramList) {
        if (uri == null) return;    //URL异常
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setConfig(requestConfig);
        /*装配请求参数*/
        String str_response = "";
        CloseableHttpResponse response = null;
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList, UTF_8);
            httpPost.setEntity(entity); //设置请求参数
            response = httpClient.execute(httpPost);
            // TODO 获取cookies
//            Cookie[] cookies = httpClient.
            str_response = accessText(response);
        } catch (IOException e) {
            System.err.println("POST请求异常：" + e.getMessage());
        } finally {
            HttpClientUtils.closeQuietly(response);
            HttpClientUtils.closeQuietly(httpClient);
        }
//        System.out.println(str_response);
    }

    /**
     * 返回请求结果文本
     * @param response
     * @return
     * @throws IOException 内容转换异常
     */
    public static String accessText (CloseableHttpResponse response) {
        String str_response = "";
        HttpEntity entity = response.getEntity();
        try {
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    str_response = EntityUtils.toString(response.getEntity(), UTF_8); //获取返回结果
                    System.out.println(entity.getContentType().toString());
                    break;
                default:
                    System.err.println("连接异常：" + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            System.err.println("响应实体异常：" + e.getMessage());
        } finally {
            try {
                if (response != null) response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return str_response;
    }
}
