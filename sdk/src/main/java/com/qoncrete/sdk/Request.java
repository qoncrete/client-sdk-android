package com.qoncrete.sdk;

import android.content.Context;
import android.util.Log;

import com.qoncrete.okhttp3.Call;
import com.qoncrete.okhttp3.ConnectionPool;
import com.qoncrete.okhttp3.MediaType;
import com.qoncrete.okhttp3.OkHttpClient;
import com.qoncrete.okhttp3.RequestBody;
import com.qoncrete.okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */

class Request {
    private static final String DOMAIN = "www.baidu.com";
    //    private static final String DOMAIN = "192.168.1.5:8990/json";
    private static String URL = "http://" + DOMAIN;
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;

    /**
     * @param secureTransport 通过SSL发送日志 默认 false
     * @param cacheDNS        DNS 缓存 默认 true
     * @param connectTimeout  连接超时 默认 15 (S)
     * @param retryOnTimeout  登录超时重发次数 默认 1
     * @param concurrency     并发数 默认 10
     */
    Request(Context context, boolean secureTransport, boolean cacheDNS, int connectTimeout, int retryOnTimeout, int concurrency) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(concurrency, 5L, TimeUnit.MINUTES))
                .addInterceptor(new RetryInterceptor(retryOnTimeout));
        URL = (secureTransport ? "https://" : "http://") + DOMAIN;
        // TODO 问题：使用代理会异常
        if (cacheDNS) {
            DNSCache.init(context);
            builder.dns(DNSCache.getInstance().HTTP_DNS);
        }
        client = builder.build();
    }

    void send(Object obj, Callback callback) {
        com.qoncrete.okhttp3.Request request = buildRequest(obj);
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                callback.onResponse(response.body().string(), response.receivedResponseAtMillis() - response.sentRequestAtMillis());
            } else {
                callback.onFailure(null, new IOException("Unexpected code " + response));
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailure(e);
        }
    }

    void asyncSend(Object obj, Callback callback) {
        com.qoncrete.okhttp3.Request request = buildRequest(obj);
        client.newCall(request).enqueue(callback);
    }

    void cancel() {
        if (client != null) {
            client.dispatcher().cancelAll();
        }
    }

    private String listToString(List logs) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        int size = logs.size();
        for (int i = 0; i < size; i++) {
            sb.append(logs.get(i));
            if (i != size - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        Log.e(TAG, "listToString: " + sb.toString());
        return sb.toString();
    }

    private com.qoncrete.okhttp3.Request buildRequest(Object obj) {
        RequestBody body;
        if (obj instanceof List) {
            body = RequestBody.create(JSON, listToString((List<String>) obj));
        } else {
            body = RequestBody.create(JSON, (String) obj);
        }
        return new com.qoncrete.okhttp3.Request.Builder()
                .url(URL)
                .post(body)
                .tag(System.currentTimeMillis())
                .build();
    }

    //        try {
//        RequestBody body = RequestBody.create(JSON, json);
//        Request request = new Request.Builder()
//                .url(URL)
//                .post(body)
//                .build();
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//
//            }
//        });
//            return response.body().string();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }

    static class Callback implements com.qoncrete.okhttp3.Callback {

        String json;

        @Override
        public void onFailure(Call call, IOException e) {

        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {

        }

        void onResponse(String res, long time) {
        }

        public void onFailure(Exception e) {
        }


    }
}
