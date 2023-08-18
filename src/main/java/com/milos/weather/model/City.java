package com.milos.weather.model;

public class City {

    private String cityName;
    private double longitude;
    private double latittude;

    public City(String cityName, double longitude, double latittude) {
        this.cityName = cityName;
        this.longitude = longitude;
        this.latittude = latittude;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatittude() {
        return latittude;
    }

    public void setLatittude(double latittude) {
        this.latittude = latittude;
    }
}
