package com.milos.weather.model;

import java.util.List;

public class Weather {
    private List<WeatherForecast> weatherForecastFor5Days;
    private int currentTemperature, windSpeed, weatherCode,
            currentApparentTemperature,
            precipitation,
            humidity,
            minTempOfDay,
            maxTempOfDay;
    private double latitude, longitude, windDirection;
    private boolean isDay;
    public Weather(List<WeatherForecast> weatherForecastFor5Days, int currentTemperature, int windSpeed, int weatherCode, int currentApparentTemperature, int precipitation, int humidity, int minTempOfDay, int maxTempOfDay, double latitude, double longitude, double windDirection, boolean isDay) {
        this.weatherForecastFor5Days = weatherForecastFor5Days;
        this.currentTemperature = currentTemperature;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.currentApparentTemperature = currentApparentTemperature;
        this.precipitation = precipitation;
        this.humidity = humidity;
        this.minTempOfDay = minTempOfDay;
        this.maxTempOfDay = maxTempOfDay;
        this.latitude = latitude;
        this.longitude = longitude;
        this.windDirection = windDirection;
        this.isDay = isDay;
    }
    public int getCurrentTemperature() {
        return currentTemperature;
    }
    public int getWindSpeed() {
        return windSpeed;
    }
    public int getWeatherCode() {
        return weatherCode;
    }
    public double getWindDirection() {
        return windDirection;
    }
    public int getPrecipitation() {
        return precipitation;
    }
    public int getHumidity() {
        return humidity;
    }
    public int getMinTempOfDay() {
        return minTempOfDay;
    }
    public int getMaxTempOfDay() {
        return maxTempOfDay;
    }
    public List<WeatherForecast> getWeatherForecastFor5Days() {
        return weatherForecastFor5Days;
    }
    public boolean isDay() {
        return isDay;
    }
}
