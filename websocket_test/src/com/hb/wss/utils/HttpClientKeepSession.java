package com.hb.wss.utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hongbo on 2017/6/27.
 */
public class HttpClientKeepSession {
    static Logger logger = LogManager.getLogger(HttpClientKeepSession.class.getName());
    public static CloseableHttpClient httpClient = null;
    public static HttpClientContext context = null;
    public static CookieStore cookieStore = null;
    public static RequestConfig requestConfig = null;

    protected static final String UTF_8 = HTTP.UTF_8;
    static {
        init();
    }

    private static void init() {
        context = HttpClientContext.create();
        cookieStore = new BasicCookieStore();
        // 配置超时时间（连接服务端超时1分，请求数据返回超时2分）
        requestConfig = RequestConfig.custom().setConnectTimeout(720000).setSocketTimeout(360000)
                .setConnectionRequestTimeout(360000).build();
        // 设置默认跳转以及存储cookie
        httpClient = HttpClientBuilder.create()
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                .setRedirectStrategy(new DefaultRedirectStrategy()).setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore).build();
    }

    /**
     * http get
     *
     * @param url
     * @return response
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static CloseableHttpResponse get(String url) throws IOException {
        HttpGet httpget = new HttpGet(url);
        CloseableHttpResponse response = httpClient.execute(httpget, context);
        try {
            cookieStore = context.getCookieStore();
            printCookies();
        } finally {
            response.close();
        }
        return response;
    }

    /**
     * http post
     *
     * @param url
     * @param parameters
     *            form表单
     * @return response
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static CloseableHttpResponse post(String url, String parameters)
            throws IOException {
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvps = toNameValuePairList(parameters);
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        CloseableHttpResponse response = httpClient.execute(httpPost, context);
        try {
            cookieStore = context.getCookieStore();
            printCookies();
        } finally {
            response.close();
        }
        return response;

    }

    public static void upload(String url) {
        try {
            HttpPost httppost = new HttpPost(url);
            FileBody bin = new FileBody(new File("B://qmcb_170621192239_40W_16min/qmcb_1.xls"));
            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addPart("importExcelData", bin)
                    .setCharset(CharsetUtils.get("UTF-8")).build();
            httppost.setEntity(reqEntity);
            httppost.setEntity(new UrlEncodedFormEntity(toNameValuePairList("serviceid=saveExcelDatasService&loginname=lf_1&password=21218cca77804d2ba1922c33e0151105&params=[{\\\"aab001\\\":\\\"141024100234\\\",\\\"aab004\\\":\\\"洪洞县大槐树镇大胡麻村委会\\\",\\\"aab019\\\":\\\"\\\",\\\"aab301\\\":\\\"141024\\\",\\\"aac001\\\":\\\"\\\",\\\"aac002\\\":\\\"141024196805110013\\\",\\\"aac003\\\":\\\"郭玉龙\\\",\\\"aac004\\\":\\\"1\\\",\\\"aac005\\\":\\\"01\\\",\\\"aac006\\\":\\\"19680511\\\",\\\"aac009\\\":\\\"20\\\",\\\"aac010\\\":\\\"山西省临汾市洪洞县\\\",\\\"aac011\\\":\\\"595\\\",\\\"aac013\\\":\\\"003\\\",\\\"aac016\\\":\\\"0\\\",\\\"aac060\\\":\\\"1\\\",\\\"aae005\\\":\\\"13293779218\\\",\\\"aae006\\\":\\\"山西省临汾市洪洞县大槐树镇大胡麻村委会\\\",\\\"aae014\\\":\\\"\\\",\\\"aaz070\\\":\\\"141024100234\\\",\\\"aic003\\\":\\\"102_12\\\",\\\"ajc003\\\":\\\"\\\",\\\"akc003\\\":\\\"303_23\\\",\\\"alc003\\\":\\\"\\\",\\\"amc003\\\":\\\"\\\"}]")));
            System.out.println("executing request: "+ httppost.getRequestLine());
            CloseableHttpResponse response = httpClient.execute(httppost,context);
            try {
                cookieStore = context.getCookieStore();
                List<Cookie> cookies = cookieStore.getCookies();
                for (Cookie cookie : cookies) {
//                    LOG.debug("key:" + cookie.getName() + "  value:" + cookie.getValue());
                    System.out.println("key:" + cookie.getName() + "  value:" + cookie.getValue());

                }

                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    // 响应长度
                    System.out.println("Response content length: "
                            + resEntity.getContentLength());
                    // 打印响应内容
                    System.out.println("Response content: "
                            + EntityUtils.toString(resEntity));
                }
                // 销毁
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<NameValuePair> toNameValuePairList(String parameters) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        String[] paramList = parameters.split("&");
        for (String parm : paramList) {
            int index = -1;
            for (int i = 0; i < parm.length(); i++) {
                index = parm.indexOf("=");
                break;
            }
            String key = parm.substring(0, index);
            String value = parm.substring(++index, parm.length());
            nvps.add(new BasicNameValuePair(key, value));
        }
        System.out.println(nvps.toString());
        return nvps;
    }

    /**
     * 手动增加cookie
     * @param name
     * @param value
     * @param domain
     * @param path
     */
    public static void addCookie(String name, String value, String domain, String path) {
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookieStore.addCookie(cookie);
    }

    /**
     * 把结果console出来
     *
     * @param httpResponse
     * @throws ParseException
     * @throws IOException
     */
    public static void printResponse(HttpResponse httpResponse) throws ParseException, IOException {
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        // 响应状态
        System.out.println("status:" + httpResponse.getStatusLine());
        System.out.println("headers:");
        HeaderIterator iterator = httpResponse.headerIterator();
        while (iterator.hasNext()) {
            System.out.println("\t" + iterator.next());
        }
        /* 流不可重复读取：Attempted read from closed stream.
        // 判断响应实体是否为空
        if (entity != null) {
            String responseString = EntityUtils.toString(entity);
            System.out.println("response length:" + responseString.length());
            System.out.println("response content:" + responseString.replace("\r\n", ""));
        }
        */
        System.out.println("------------------------------------------------------------------------------------------\r\n");
    }

    /**
     * 把当前cookie从控制台输出出来
     *
     */
    public static void printCookies() {
        System.out.println("headers:");
        cookieStore = context.getCookieStore();
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            System.out.println("key:" + cookie.getName() + "  value:" + cookie.getValue());
        }
    }

    /**
     * 检查cookie的键值是否包含传参
     *
     * @param key
     * @return
     */
    public static boolean checkCookie(String key) {
        cookieStore = context.getCookieStore();
        List<Cookie> cookies = cookieStore.getCookies();
        boolean res = false;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                res = true;
                break;
            }
        }
        return res;
    }

    /**
     * 直接把Response内的Entity内容转换成String
     *
     * @param httpResponse
     * @return
     * @throws ParseException
     * @throws IOException
     */
    public static String toString(CloseableHttpResponse httpResponse) throws ParseException, IOException {
        // 获取响应消息实体
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null)
            return EntityUtils.toString(entity);
        else
            return null;
    }

    /**
     * 模拟AJAX发起post请求
     * @param url
     * @param json
     * @return
     */
    public static JSONObject post (String url, JSONObject json) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpResponse response = null;
        JSONObject jsonObject = null;
        try {
            if (json == null) return null;  //判断参数是否为空
            StringEntity entity = new StringEntity(json.toString(), Charset.forName(UTF_8));
            entity.setContentEncoding(UTF_8);
            entity.setContentType("application/json;charset=UTF-8");
            httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
            httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpPost.setHeader("Content-Type", "multipart/form-data;charset=UTF-8");

            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost, context);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                cookieStore = context.getCookieStore();
//                printCookies();
//                printResponse(response);
                String result = EntityUtils.toString(response.getEntity(), HttpClientKeepSession.UTF_8);
                jsonObject = JSONObject.fromObject(result);
//                System.out.println(result);
                logger.debug(result);
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("JSON转换异常：" + e.getMessage());
        } catch (ClientProtocolException e) {
            logger.error("执行POST请求异常1：" + e.getMessage());
        } catch (IOException e) {
            logger.error("执行POST请求异常2：" + e.getMessage());
            throw e;
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
        return jsonObject;
    }

    public static void main(String[] args) throws IOException {
    }
}
