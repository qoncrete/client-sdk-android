package com.qoncrete.sdk;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import com.qoncrete.okhttp3.Call;
import com.qoncrete.okhttp3.Response;

import static android.content.ContentValues.TAG;

//const client = new Qsdk.QoncreteClient({
//        sourceID: 'SOURCE_ID', // MANDATORY: The source ID. (Once logged-in, can be found at https://qoncrete.com/account/#!/source)
//        apiToken: 'API_TOKEN',// MANDATORY: The api token. (Once logged-in, can be found at https://qoncrete.com/account/#!/token)
//        errorLogger: (err) => {},  // A function called on error. Default: (err) => {}
//        通过SSL发送日志。默认值：false
//        secureTransport: false, // Send log over SSL. Default: false
//        活跃用户的DNS缓存
//        cacheDNS: true, // Active userland dns cache. Default: true"
//        超时
//        timeoutAfter: 15000, // Abort the query on timeout. Default: 15s
//        登录超时重发次数。默认值：1（暂停时，它将重试一次）
//        retryOnTimeout: 1, // Number of times to resend the log on timeout. Default: 1 (on timeout, it will retry one more time)
//        尝试用批量发送日志，而不是一个一个发送。默认值：true
//        autoBatch: true, // Try to send log by batch instead of sending them one by one. Default: true
//        打包数量
//        batchSize: 1000, // Only matters if autoBatch is True. Number of logs to send in a batch. Default: 1000, Max: 1000
//        autoSendAfter: 2000, // Only matters if autoBatch is True. Time after the logs will be sent if the batch is not full. Default: 2s
//        并发数
//        concurrency: 200 // Number of simultaneous queries that can be made, can be set lower or higher depending your server configuration. Default: 200
//        })


/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */

final public class Qoncrete {

    private PollingThread polling;
    private Request request;
    private Request.Callback reqCallback;
    private Qoncrete.Callback apiCallback;
    private DB db;

    private Context applicationContext;
    private String sourceID;
    private String apiToken;
    // 通过SSL发送日志 默认 false
    private boolean secureTransport;
    // DNS 缓存 默认 true
    private boolean cacheDNS;
    // 连接超时 默认 15S
    private int connectTimeout;
    // 登录超时重发次数 默认 1
    private int retryOnTimeout;
    // 批量发送日志 默认 true
    private boolean autoBatch;
    // 打包数量 默认 1000
    private int batchSize;
    // (仅 autoBatch = true 生效) 每隔多少时间发送一次，默认 2S
    private int autoSendAfter;
    // 并发数 默认 10
    private int concurrency;


    public Qoncrete(Context context, Builder builder) {
        init(context, builder);
    }

    public Qoncrete(Context context, String sourceID, String apiToken) {
        init(context, new Builder().sourceID(sourceID).apiToken(apiToken));
    }

    private void init(Context context, Builder builder) {
        if (context != null) {
            this.applicationContext = context.getApplicationContext();
        }
        this.sourceID = builder.sourceID;
        this.apiToken = builder.apiToken;
        this.secureTransport = builder.secureTransport;
        this.cacheDNS = builder.cacheDNS;
        this.connectTimeout = builder.connectTimeout;
        this.retryOnTimeout = builder.retryOnTimeout;
        this.autoBatch = builder.autoBatch;
        this.batchSize = builder.batchSize;
        this.autoSendAfter = builder.autoSendAfter;
        this.concurrency = builder.concurrency;

        request = new Request(this.applicationContext, this.secureTransport, this.cacheDNS, this.connectTimeout, this.retryOnTimeout, this.concurrency);
        reqCallback = new Request.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: " + e.toString());
                onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e(TAG, "onResponse: ");
                onResponse("res", response.receivedResponseAtMillis() - response.sentRequestAtMillis());
            }

            @Override
            void onResponse(String res, long time) {
                Log.e(TAG, "onResponse2: ");
                if (apiCallback != null) {
                    apiCallback.onResponse(0);
                }
            }

            @Override
            public void onFailure(Exception e) {
                super.onFailure(e);
                Log.e(TAG, "onFailure: " + e.toString());
                if (apiCallback != null) {
                    apiCallback.onFailure();
                }
            }
        };

        // 批量发送日志
        if (autoBatch) {
            db = DB.getInstance(this.applicationContext);
            initPollThread();
        }
    }

    private void initPollThread() {
        polling = new PollingThread(this.autoSendAfter, new PollingThread.Listener() {
            @Override
            public boolean hasLog() {
                System.out.println("hasLog");
                return db.logCount() > 0;
            }

            @Override
            public void sendLog() {
                // 同步发送
                List logs = db.getAllLogs();
                request.send(logs, reqCallback);
            }
        });
        polling.start();
    }

    public void setCallback(Qoncrete.Callback callback) {
        this.apiCallback = callback;
    }

    public void send(String json) {
        System.out.println("send");
        // 判断是否有网络
        if (!Utils.isNetworkConnected(applicationContext)) {
            if (apiCallback != null) {
                apiCallback.onFailure();
            }
            return;
        }
        // 批量发送日志
        if (autoBatch) {
            // 如果log数量达到临界值，发送log
            if (db.logCount() + 1 >= batchSize) {
                // 初始化轮询
                polling.closeThread();
                initPollThread();

                List logs = db.getAllLogs();
                logs.add(json);
                request.asyncSend(logs, reqCallback);
            } else {
                db.putLog(json);
                // 通知 开始轮询
                polling.notifyPoll();
            }
        } else {
            // 异步发送
            request.asyncSend(json, reqCallback);
        }
    }

    public void destroy() {
        if (polling != null) {
            polling.closeThread();
        }
        if (db != null) {
            db.close();

            // TODO 销毁?
            db.destroy();
        }
    }

    public interface Callback {
        void onFailure();

        void onResponse(long time);
    }

    public static final class Builder {

        String sourceID;
        String apiToken;
        // 通过SSL发送日志 默认 false
        boolean secureTransport;
        // DNS 缓存 默认 true
        boolean cacheDNS;
        // 连接超时 默认 15S
        int connectTimeout;
        // 登录超时重发次数 默认 1
        int retryOnTimeout;
        // 批量发送日志 默认 true
        boolean autoBatch;
        // 打包数量 默认 1000
        int batchSize;
        // (仅 autoBatch = true 生效) 每隔多少时间发送一次，默认 2S
        int autoSendAfter;
        // 并发数 默认 10
        int concurrency;

        public Builder() {
            this.secureTransport = false;
            this.cacheDNS = true;
            this.connectTimeout = 15;
            this.retryOnTimeout = 1;
            this.autoBatch = true;
            this.batchSize = 100;
            this.autoSendAfter = 2;
            this.concurrency = 10;
        }

        public Builder sourceID(String sourceID) {
            this.sourceID = sourceID;
            return this;
        }

        public Builder apiToken(String apiToken) {
            this.apiToken = apiToken;
            return this;
        }

        public Builder secureTransport(boolean secureTransport) {
            this.secureTransport = secureTransport;
            return this;
        }

        public Builder cacheDNS(boolean cacheDNS) {
            this.cacheDNS = cacheDNS;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder retryOnTimeout(int retryOnTimeout) {
            this.retryOnTimeout = retryOnTimeout;
            return this;
        }

        public Builder autoBatch(boolean autoBatch) {
            this.autoBatch = autoBatch;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder autoSendAfter(int autoSendAfter) {
            this.autoSendAfter = autoSendAfter;
            return this;
        }

        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

//        public Qoncrete build() {
//            return new Qoncrete(this);
//        }

        public Qoncrete build(Context context) {
            return new Qoncrete(context, this);
        }
    }
}
