package com.milos.weather.model;

/**
 * Weather forecast for next 5 days
 */
public class WeatherForecast {
    private String dayName;
    private int maxTemperature, minTemperature, weatherCode;


    public WeatherForecast(String dayName, int max, int min, int weatherCode) {
        this.dayName = dayName;
        this.maxTemperature = max;
        this.minTemperature = min;
        this.weatherCode = weatherCode;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(int maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(int minTemperature) {
        this.minTemperature = minTemperature;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
    }
}
