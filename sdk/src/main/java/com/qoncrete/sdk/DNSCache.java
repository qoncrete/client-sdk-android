package com.qoncrete.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qoncrete.okhttp3.Cache;
import com.qoncrete.okhttp3.Dispatcher;
import com.qoncrete.okhttp3.Dns;
import com.qoncrete.okhttp3.HttpUrl;
import com.qoncrete.okhttp3.Interceptor;
import com.qoncrete.okhttp3.OkHttpClient;
import com.qoncrete.okhttp3.Request;
import com.qoncrete.okhttp3.Response;

import static android.content.ContentValues.TAG;

/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */

class DNSCache {
    private static DNSCache dnsCache;
    private OkHttpClient httpDNSClient;
    private Dispatcher dispatcher;
    private static Context context;
    private static final long CACHE_TIME = 600;

    static void init(Context context) {
        DNSCache.context = context;
    }

    public static DNSCache getInstance() {
        if (dnsCache == null) {
            dnsCache = new DNSCache();
        }
        return dnsCache;
    }

    Dns HTTP_DNS = new Dns() {

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            Log.e(TAG, "lookup:" + hostname);

            if (hostname == null) throw new UnknownHostException("hostname == null");
            //dnspod提供的dns服务
            HttpUrl httpUrl = new HttpUrl.Builder().scheme("http")
                    .host("119.29.29.29")
                    .addPathSegment("d")
                    .addQueryParameter("dn", hostname)
                    .build();
            Request dnsRequest = new Request.Builder().url(httpUrl).get().build();

            long start = System.currentTimeMillis();
            try {
                String s = getHTTPDnsClient().newCall(dnsRequest).execute().body().string();
                Log.e(TAG, "lookup: getDNS " + s + " time:" + (System.currentTimeMillis() - start));
                if (!TextUtils.isEmpty(s)) {
                    String[] ips = s.split(";");
                    if (ips.length >= 1) {
                        Log.e(TAG, "lookup: find host:" + ips[0]);
                        return Arrays.asList(InetAddress.getAllByName(ips[0]));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Dns.SYSTEM.lookup(hostname);
        }
    };

    private synchronized OkHttpClient getHTTPDnsClient() {
        if (httpDNSClient == null) {
            final File cacheDir = context.getExternalCacheDir();
            httpDNSClient = new OkHttpClient.Builder()
                    //消费者工作线程池
                    .dispatcher(getDispatcher())
                    .addNetworkInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response originalResponse = chain.proceed(chain.request());
                            return originalResponse.newBuilder()
                                    //在返回header中加入缓存消息
                                    //下次将不再发送请求
                                    .header("Cache-Control", "max-age=" + CACHE_TIME).build();
                        }
                    })
                    //1MB的文件缓存
                    .cache(new Cache(new File(cacheDir, "httpdns"), 1 * 1024 * 1024))
                    .build();
        }
        return httpDNSClient;
    }

    private synchronized Dispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = new Dispatcher();
        }
        return dispatcher;
    }

    boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        String rexp = "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))";

        // return addr.matches(rexp);

        Pattern pat = Pattern.compile(rexp);

        Matcher mat = pat.matcher(addr);

        boolean ipAddress = mat.matches();
        return ipAddress;

    }
}
