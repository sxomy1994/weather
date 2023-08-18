package com.milos.weather.utils;

import android.text.TextUtils;
import android.util.Log;

import com.milos.weather.model.City;
import com.milos.weather.model.Weather;
import com.milos.weather.model.WeatherForecast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QueryUtils {
    private  QueryUtils(){}
    public static URL createURL(String sURL){
        try {
            return new URL(sURL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    private static String readFromStream(InputStream stream){
        StringBuilder builder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = bufferedReader.readLine()) != null){
                builder.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    public static String makeHttpRequest(URL url) throws IOException {
        String json = "";
        HttpURLConnection connection = null;
        InputStream stream =null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode == 200){
                stream = connection.getInputStream();
                json =readFromStream(stream);
            }else{
                Log.e("QueryUtils","Greska response code:" + responseCode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if(connection!=null){
                connection.disconnect();
            }
            if(stream !=null){
                stream.close();
            }
        }
        return json;
    }
    public static Weather extractWeatherData(String json){
        if(TextUtils.isEmpty(json)) return null;
        try {
            JSONObject root = new JSONObject(json);
            double latitude = root.getDouble("latitude");
            double longitude = root.getDouble("longitude");
            JSONObject currentWeather = root.getJSONObject("current_weather");
            JSONObject hourly = root.getJSONObject("hourly");
            JSONArray apparentTemperature = hourly.getJSONArray("apparent_temperature");
            JSONArray precipitationProbabilityJsonArray = hourly.getJSONArray("precipitation_probability");
            JSONArray temperature2mJsonArray = hourly.getJSONArray("temperature_2m");
            JSONArray humidityJsonArray = hourly.getJSONArray("relativehumidity_2m");
            int temperature = Math.round((int)currentWeather.getDouble("temperature"));
            int windSpeed = Math.round((int)currentWeather.getDouble("windspeed"));
            int weatherCode = currentWeather.getInt("weathercode");
            boolean isDay = checkIfIsDay(currentWeather.getInt("is_day"));
            double windDirection = currentWeather.getDouble("winddirection");
            int currentApparentTemperature = 0, precipitationProbability = 0, humidity = 0;
            int minTempOfDay = (int)Math.round(minAndMaxTempOfDay(temperature2mJsonArray)[0]);  //Prvi clan niza pomocne metode je min
            int maxTempOfDay = (int)Math.round(minAndMaxTempOfDay(temperature2mJsonArray)[1]);  //Drugi clan niza je max
            JSONObject daily = root.getJSONObject("daily");
            JSONArray weatherCodeForecastJsonArray = daily.getJSONArray("weathercode");
            JSONArray temperature2mMaxForecastJSONArray = daily.getJSONArray("temperature_2m_max");
            JSONArray temperature2mMinForecastJsonArray = daily.getJSONArray("temperature_2m_min");
            List<WeatherForecast> weatherForecastFor5Days =
                    weatherForecasts(temperature2mMaxForecastJSONArray,
                            temperature2mMinForecastJsonArray,weatherCodeForecastJsonArray);
            for (int i = 0; i < apparentTemperature.length();i++){
                int currentHour = LocalDateTime.now().getHour();
                if(currentHour == i){
                    currentApparentTemperature = (int)Math.round(apparentTemperature.getDouble(i));
                    precipitationProbability = precipitationProbabilityJsonArray.getInt(i);
                    humidity = humidityJsonArray.getInt(i);
                    break;
                }
            }
            return  new Weather(weatherForecastFor5Days, temperature,windSpeed,weatherCode,currentApparentTemperature,
                    precipitationProbability, humidity, minTempOfDay, maxTempOfDay, latitude,longitude, windDirection, isDay);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    //Lista za Autocomplete TextView
    public static List<City> extractGeoDataaFromJSON(String json){
        if(TextUtils.isEmpty(json)){
            return null;
        }
        List<City>list = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray features = root.getJSONArray("features");
            for (int i = 0; i < features.length();i++){
                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
                String name = properties.getString("formatted");
                double lon = properties.getDouble("lon");
                double lat = properties.getDouble("lat");
                list.add(new City(name,lon,lat));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    private static double[] minAndMaxTempOfDay(JSONArray array) throws JSONException {
        double min = array.getDouble(0);
        double max = min;
        for (int i = 0; i <= 23;i++){
            if(array.getDouble(i) < min) min = array.getDouble(i);
            if(array.getDouble(i) > max) max = array.getDouble(i);
        }
        return new double[]{min,max};
    }
    private static List<WeatherForecast> weatherForecasts(JSONArray maxTemp,
                                                          JSONArray minTemp,
                                                          JSONArray weatherCode) throws JSONException {
        List<WeatherForecast> forecasts = new ArrayList<>();
        for (int i = 0; i < 5;i++){
            String dayName = LocalDateTime.now().plusDays(i).getDayOfWeek().toString();
           int max = (int) Math.round(maxTemp.getDouble(i));
            int min = (int) Math.round(minTemp.getDouble(i));
            int code = weatherCode.getInt(i);
            Log.i("VREME za dan " + i,"max: " + maxTemp.getDouble(i) + ",min: " + min + ", code: " + code );

            forecasts.add(new WeatherForecast(dayName,max,min,code));
        }
        return forecasts;
    }
    //Proveravamo da li je dan ili noc, 1 je za dan , 0 za noc
    private static boolean checkIfIsDay(int codeNumber){
            return codeNumber==1;
    }

}
