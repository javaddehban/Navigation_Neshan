package com.example.navigationneshan.data.repository;


import com.example.navigationneshan.data.api.ApiInterface;
import com.example.navigationneshan.data.model.SearchModel;

import org.neshan.common.model.LatLng;
import org.neshan.servicessdk.direction.model.NeshanDirectionResult;

import javax.inject.Inject;

import io.reactivex.Observable;

public class ApiRepo {

    ApiInterface apiInterface;

    @Inject
    public ApiRepo(ApiInterface apiInterface) {
        this.apiInterface = apiInterface;
    }

    public Observable<NeshanDirectionResult> getMatrixDistance(LatLng userLatLng, LatLng currentLatLng) {
        String userLocation = userLatLng.getLatitude() + "," + userLatLng.getLongitude();
        String currentLocation = currentLatLng.getLatitude() + "," + currentLatLng.getLongitude();
        return apiInterface.getDistance(userLocation, currentLocation);

    }

    public Observable<SearchModel> getSearchQuery(String searchQuery, LatLng userLatLng) {
        return apiInterface.getSearchResult(searchQuery, String.valueOf(userLatLng.getLatitude()), String.valueOf(userLatLng.getLongitude()));

    }


}
