package com.ruoyi.huiyi.util;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用 HTTP 请求工具类。
 * 不绑定任何具体业务，也不自动读取配置。
 * 调用方传入URL、参数，发请求，返回原始响应字符串。
 */
public class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 60000;

    private static final CloseableHttpClient HTTP_CLIENT;

    static {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(20);
        HTTP_CLIENT = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    private HttpClientUtil() {
    }

    // GET
    public static String doGet(String url, Map<String, String> params) throws IOException {
        return doGet(url, params, null, CONNECT_TIMEOUT, SOCKET_TIMEOUT);
    }

    public static String doGet(String url, Map<String, String> params,
                                   Map<String, String> headers,
                                   int connectTimeout, int socketTimeout) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url);
            if (params != null) {
                params.forEach(builder::addParameter);
            }
            HttpGet httpGet = new HttpGet(builder.build());
            httpGet.setConfig(buildRequestConfig(connectTimeout, socketTimeout));
            addHeaders(httpGet, headers);
            return execute(httpGet);
        }catch (URISyntaxException e) {
            throw new IOException("非法的URL: " + url, e);
        }
    }

    /**
     *通用的POST请求方法(发送JSON数据)
     * * @Param url
     * @Param jsonParams 转换为JSON字符串的参数
     * @return 接口返回的相应字符串
     **/
    // POST JSON
    public static String doPostJson(String url, String jsonParam) throws IOException {
        return doPostJson(url, jsonParam, null, CONNECT_TIMEOUT, SOCKET_TIMEOUT);
    }

    public static String doPostJson(String url, String jsonBody, Map<String,String> headers,
                                    int connectTimeout, int socketTimeout) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(buildRequestConfig(connectTimeout, socketTimeout));
        httpPost.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        addHeaders(httpPost, headers);
        return execute(httpPost);
    }

    // POST FORM
    public static String doPostForm(String url, Map<String, String> params) throws IOException {
        return doPostForm(url, params, null, CONNECT_TIMEOUT, SOCKET_TIMEOUT);
    }

    public static String doPostForm(String url, Map<String, String> params,
                                    Map<String, String> headers,
                                    int connectTimeout, int socketTimeout) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(buildRequestConfig(connectTimeout, socketTimeout));
        if (params != null) {
            List<NameValuePair> pairs = new ArrayList<>();
            params.forEach((k, v) -> pairs.add(new BasicNameValuePair(k, v)));
            httpPost.setEntity(new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8));
        }
        addHeaders(httpPost, headers);
        return execute(httpPost);
    }

    // POST Multipart
    /**
     * @param fileFieldName 文件对应的表单字段名
     * @param file          待上传文件
     * @param extraParams   除文件外的其它表单参数
     */
    public static String doPostFile(String url, String fileFieldName, File file,
                                    Map<String, String> extraParams) throws IOException {
        return doPostFile(url, fileFieldName, file, extraParams, null, CONNECT_TIMEOUT, SOCKET_TIMEOUT);
    }

    public static String doPostFile(String url, String fileFieldName, File file,
                                    Map<String, String> extraParams,
                                    Map<String, String> headers,
                                    int connectTimeout, int socketTimeout) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(buildRequestConfig(connectTimeout, socketTimeout));

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(fileFieldName, file);
        if (extraParams != null) {
            extraParams.forEach((k, v) ->
                    builder.addTextBody(k, v, ContentType.create("text/plain", StandardCharsets.UTF_8)));
        }
        httpPost.setEntity(builder.build());
        addHeaders(httpPost, headers);
        return execute(httpPost);
    }

    // Private Function
    private static RequestConfig buildRequestConfig(int connectTimeout, int socketTimeout) {
        return RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(connectTimeout)
                .build();
    }

    private static void addHeaders(HttpRequestBase request, Map<String, String> headers) {
        if(headers != null) {
            headers.forEach(request::setHeader);
        }
    }

    private static String execute(HttpRequestBase request) throws IOException{
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            HttpEntity entity = response.getEntity();
            String result = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : null;
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 200 && statusCode < 300) {
                return result;
            }
            log.warn("HTTP请求返回非2xx状态码: {}, url: {}, body: {}",
                    statusCode, request.getURI(), result);
            throw new IOException("请求失败，状态码: " + statusCode + "，响应: " + result);
        }
    }
}
