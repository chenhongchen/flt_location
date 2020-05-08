package com.mt.flt_location.googleserver;


import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 */

public class ServiceClient {
    private static volatile ServiceClient serviceClient = null;
    private Retrofit retrofit;

    /**
     * 初始化httpclient
     */
    private ServiceClient() {
        buildRetrofit();

    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * 单例构造
     *
     * @return HttpClient
     */
    public static ServiceClient getClient() {
        if (null == serviceClient) {
            synchronized (ServiceClient.class) {
                if (null == serviceClient) {
                    serviceClient = new ServiceClient();
                }
            }
        }
        return serviceClient;
    }

    private void buildRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(buildHttpClient())
                .build();

    }

    private OkHttpClient buildHttpClient() {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        //OkHttpUtils.addInterceptors(new AuInterceptor());
        return builder.build();
    }
}
