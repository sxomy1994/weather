package com.milos.weather.api;

import android.util.Log;

import com.milos.weather.model.City;
import com.milos.weather.utils.QueryUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class GeoapifyApi {
    private static final String API_KEY = "e9f390f299a7480c89cd883d587b70fe";
    public List<City> searchCityAutocomplete(String city) {
        String jsonResponse = "";
        URL url = QueryUtils.createURL(
                "https://api.geoapify.com/v1/geocode/autocomplete?" + "text=" + city +"&apiKey="+ API_KEY);
        try {
            jsonResponse = QueryUtils.makeHttpRequest(url);
            Log.i("VEZBANJE jsonRESPONSE: ", jsonResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return QueryUtils.extractGeoDataaFromJSON(jsonResponse);
    }
}
