package com.example.navigationneshan.ui.main.viewmodel;


import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.navigationneshan.data.model.SearchModel;
import com.example.navigationneshan.data.repository.ApiRepo;

import org.neshan.common.model.LatLng;
import org.neshan.servicessdk.direction.model.NeshanDirectionResult;
import org.neshan.servicessdk.direction.model.Route;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

@HiltViewModel
public class MyViewModelActivity extends ViewModel {
    public ApiRepo apiRepo;
    public MutableLiveData<Route> routeLiveData;
    public MutableLiveData<List<SearchModel.Item>> searchResultLiveData;

    @Inject
    public MyViewModelActivity(ApiRepo apiRepo) {
        this.apiRepo = apiRepo;
    }

    public MutableLiveData<Route> getRootNavigation(LatLng userLocation, LatLng currentLocation) {
        routeLiveData = new MutableLiveData<>();
        apiRepo.getMatrixDistance(userLocation, currentLocation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NeshanDirectionResult>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull NeshanDirectionResult neshanDirectionResult) {
                        routeLiveData.postValue(neshanDirectionResult.getRoutes().get(0));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        routeLiveData.postValue(null);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        return routeLiveData;
    }

    public MutableLiveData<List<SearchModel.Item>> getSearch(String searchQuery, LatLng userLatLng) {
        searchResultLiveData = new MutableLiveData<>();
        apiRepo.getSearchQuery(searchQuery, userLatLng)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<SearchModel>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull SearchModel searchModel) {

                        searchResultLiveData.postValue(searchModel.getItems());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        searchResultLiveData.postValue(null);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        return searchResultLiveData;

    }
}