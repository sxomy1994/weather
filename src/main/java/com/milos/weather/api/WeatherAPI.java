package com.milos.weather.api;

import android.util.Log;

import com.milos.weather.utils.QueryUtils;
import com.milos.weather.model.Weather;

import java.io.IOException;
import java.net.URL;

public class WeatherAPI {
    private static final String  WEATHER_REQUEST_URL = "https://api.open-meteo.com/v1/forecast?";
    public  static Weather getWeatherData (double latitude, double longitude){
        String json= "";
        StringBuilder builder = new StringBuilder(WEATHER_REQUEST_URL);
        builder.append("latitude=").append(latitude+"&")
                .append("longitude=").append(longitude)
                .append("&hourly=temperature_2m,relativehumidity_2m,apparent_temperature,precipitation_probability,weathercode,uv_index,temperature_1000hPa&daily=weathercode,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_sum&current_weather=true&timezone=auto");
        URL url = QueryUtils.createURL(builder.toString());
        try {
            json = QueryUtils.makeHttpRequest(url);
        } catch (IOException e) {
            Log.i("IOEXCEPTION VIEW MODEL", e.getMessage());
        }
        return QueryUtils.extractWeatherData(json);
    }
}
