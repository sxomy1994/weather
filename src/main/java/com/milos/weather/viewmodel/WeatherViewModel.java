package com.milos.weather.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.milos.weather.api.WeatherAPI;
import com.milos.weather.model.Weather;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WeatherViewModel extends ViewModel {
    MutableLiveData<Weather>  weatherMutableLiveData = new MutableLiveData<>();
    public void doBackgroundTask(double lat, double lon){
        Executor executor = Executors.newSingleThreadExecutor();
        Callable<Weather> callable = () -> WeatherAPI.getWeatherData(lat,lon);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Weather weather = callable.call();
                    weatherMutableLiveData.postValue(weather);
                } catch (Exception e) {
                    Log.e("WeatherViewModel", e.getMessage());
                }
            }
        });
    }
    public LiveData<Weather> getWeatherData(){
        return weatherMutableLiveData;
    }
}
