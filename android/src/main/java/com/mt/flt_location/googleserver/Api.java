package com.mt.flt_location.googleserver;

import java.util.Map;

import io.reactivex.Flowable;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface Api {
    @GET("maps/api/place/nearbysearch/json")
    Flowable<NearList> nearbysearch(@QueryMap Map<String, Object> queryMap);
}
