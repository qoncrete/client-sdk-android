package com.qoncrete.sdk;

import android.util.Log;

import com.qoncrete.okhttp3.Interceptor;
import com.qoncrete.okhttp3.Response;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */

class RetryInterceptor implements Interceptor {
    public int maxRetry;//最大重试次数
    private int retryNum = 0;

    public RetryInterceptor(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        retryNum = 0;
        com.qoncrete.okhttp3.Request request = chain.request();
        Response response = doRequest(chain, request);
        Log.d(TAG, "intercept: " + request.tag() + " maxRetry:" + maxRetry);
        while ((response == null || !response.isSuccessful()) && retryNum < maxRetry) {
            retryNum++;
            response = doRequest(chain, request);
            Log.d(TAG, "intercept: retry : " + request.tag());
        }
        return response;
    }

    private Response doRequest(Chain chain, com.qoncrete.okhttp3.Request request) {
        try {
            return chain.proceed(request);
        } catch (Exception e) {
            return null;
        }
    }
}
