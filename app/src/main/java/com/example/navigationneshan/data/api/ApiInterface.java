package com.example.navigationneshan.data.api;


import com.example.navigationneshan.data.model.SearchModel;

import org.neshan.servicessdk.direction.model.NeshanDirectionResult;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiInterface {

    @GET("/v4/direction?type=car")
    Observable<NeshanDirectionResult> getDistance(@Query("origin") String user, @Query("destination") String current);

    @GET("v1/search")
    Observable<SearchModel> getSearchResult(@Query("term") String searchQuery, @Query("lat") String lat, @Query("lng") String lng);
}
