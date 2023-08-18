package com.milos.weather.activity;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.milos.weather.R;
import com.milos.weather.api.GeoapifyAdapter;
import com.milos.weather.viewmodel.WeatherViewModel;
import com.milos.weather.model.City;
import com.milos.weather.model.Weather;
import com.milos.weather.model.WeatherForecast;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int FULL_ANIMATION_DURATION = 1000, HALF_ANIMATION_DURATION = 500;
    private static final int LOCATION_REQUEST_CODE = 100;
    int iconResourcId;
    float heightDP;
    private ImageButton searchButton;
    private AutoCompleteTextView autoCompleteTextView;
    private LinearLayout autoCompletLinearLayout, windLinearLayout, precipitationLinearLay, humidityLinearLay;
    private ConstraintLayout rootView;
    private TextView temperature,
            cityName,
            windData,
            precipitationData,
            humidityData,
            maxMinTemperature,
            dateAndTime,
            temperatureUnit;
    TextView noInternet;

    LinearLayout forecastRoot;
    private ImageView weatherImage;
    private ImageButton  settings;
    private SwipeRefreshLayout swipeRefreshLayout;
    private GeoapifyAdapter adapter;

    private WeatherViewModel viewModel;
    private SharedPreferences preferences, recreateViewsPreferences, settingsPreferences;

    private double longitude, latitude;
    SharedPreferences.Editor editor, recreateViewsEditor;
    private String weatherDescription, windUnitName;

    private ConstraintLayout containerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        initializeViews();
        checkInternetConnection();
        loadSharedPreferences();
        checkPermission();
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        adapter = new GeoapifyAdapter(this, android.R.layout.simple_list_item_1);
        autoCompleteTextView.setAdapter(adapter);
        viewModel.getWeatherData().observe(this, new Observer<Weather>() {
            @Override
            public void onChanged(Weather weather) {
                setLoadWeatherForecastPreference(weather.getWeatherForecastFor5Days());
                loadForecastPreferencestForNextFiveDay();
                checkWeatherCode(weather.getWeatherCode(), weather.isDay());     //-weather code and weather day code
                editor.putInt("temperature", weather.getCurrentTemperature());
                editor.putInt("weather_code", weather.getWeatherCode());
                editor.putFloat("wind_direction", (float) weather.getWindDirection());
                editor.putInt("wind_speed", weather.getWindSpeed());
                editor.putInt("precipitation", weather.getPrecipitation());
                editor.putInt("humidity", weather.getHumidity());
                editor.putInt("min_day_temp", weather.getMinTempOfDay());
                editor.putInt("max_day_temp", weather.getMaxTempOfDay());
                editor.putBoolean("is_day", weather.isDay());
                editor.apply();
                temperature.setText(convertTemperature(preferences.getInt("temperature", 0)) + "");
                humidityData.setText(preferences.getInt("humidity", 0) + "%");
                windData.setText(setSpeedWind(preferences.getInt("wind_speed", 0)) + findWindDirection(preferences.getFloat("wind_direction", 0.0f)));
                cityName.setText(preferences.getString("city", ""));
                precipitationData.setText(preferences.getInt("precipitation", 0) + "%");
                maxMinTemperature.setText(weatherDescription + " " + convertTemperature(preferences.getInt("max_day_temp", 0)) + "° / " +
                        convertTemperature(preferences.getInt("min_day_temp", 0)) + "°");
                setDateAndTime();
            }
        });


        //Iskljucujemo tastaturu kada se klikne na autoComplete Text view da bi smo je omogucili kada animacija bude gotova
        autoCompleteTextView.setShowSoftInputOnFocus(false);


        //Ovo nam treba jer cekamo da android ucita UI podatke  tako da width ima neku odredjenu vrednost
        //inace ce imati vrednost 0 https://stackoverflow.com/questions/52494270/understanding-getviewtreeobserver
        autoCompletLinearLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                int width = autoCompletLinearLayout.getWidth();
                int targetWidth = settings.getLeft() - autoCompletLinearLayout.getLeft() - 50;

                autoCompletLinearLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        collapseEditText(width);
                        autoCompleteTextView.clearFocus();
                        checkInternetConnection();
                        longitude = adapter.getCity(position).getLongitude();
                        latitude = adapter.getCity(position).getLatittude(); //autoCompleteTextView onItemClickListener
                        editor.putFloat("longitude", (float) longitude);
                        editor.putFloat("latitude", (float) latitude);      //autoComplete put in preferences
                        editor.apply();
                        storeCityName();
                        viewModel.doBackgroundTask(latitude, longitude);    //autoComplete viewModel
                        Log.i("Vezbanje", "Temperatura u onItemClick : " + preferences.getInt("temperature", 0));
                        setViewsVisible();
                    }
                });

                autoCompletLinearLayout.setOnClickListener(e -> {
                    // Set focus listener on autoCompleteTextView
                    autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) {
                            collapseEditText(width);
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            Log.i("FOKUS", "NEMA FOKUS setOnFocusChangeListener ");
                        } else {
                            expandEditText(width, targetWidth);
                            Log.i("FOKUS", "IMA setOnFocusChangeListener");
                        }
                    });
                    autoCompleteTextView.requestFocus();    //Fokusiramo na autocompleteTextview
                });
            }
        });

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            viewModel.doBackgroundTask(preferences.getFloat("latitude", 0.0f),
                    preferences.getFloat("longitude", 0.0f));
        }
        searchButton.setOnClickListener(e -> {
            autoCompletLinearLayout.performClick();     //Potrebno je za animaciju jer linear layout u app baru nece da se ekspanduje
            checkBackgroundOfSearchButton();
        });
        loadForecastPreferencestForNextFiveDay(); // Kada internet nije dostupan ocitavamo prognozu poslednjeg azuriranja
        swipeAndRefresh();
        openSettingActivity();
        showToolTipText();
        changeLanguage();
    }


    private void initializeViews() {
        temperature = findViewById(R.id.temperature);
        cityName = findViewById(R.id.cityName);
        autoCompleteTextView = findViewById(R.id.add_city_image_button);
        settings = findViewById(R.id.settings_image_button);
        autoCompletLinearLayout = findViewById(R.id.add_city_linear_layout);
        searchButton = findViewById(R.id.search_image_button);
        rootView = findViewById(R.id.rootView);
        weatherImage = findViewById(R.id.weather_photo);
        windData = findViewById(R.id.wind_data_text_view);
        precipitationData = findViewById(R.id.precipitation_data_text_view);
        humidityData = findViewById(R.id.humidity_data_text_view);
        maxMinTemperature = findViewById(R.id.max_min_temp_of_current_day);
        dateAndTime = findViewById(R.id.day_and_time_of_last_temperature_change);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        windLinearLayout = findViewById(R.id.wind_linear_layout);
        precipitationLinearLay = findViewById(R.id.precipitation_linear_layout);
        humidityLinearLay = findViewById(R.id.humidity_linear_layout);
        temperatureUnit = findViewById(R.id.temperature_scale);
        forecastRoot = findViewById(R.id.forecast_root_layout);
        preferences = getSharedPreferences("cityWeatherData", MODE_PRIVATE);
        editor = preferences.edit();
        recreateViewsPreferences = getSharedPreferences("weatherForecastForFiveDays", Context.MODE_PRIVATE);
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        recreateViewsEditor = recreateViewsPreferences.edit();
        noInternet = findViewById(R.id.no_internet_text_view);
        containerLayout = findViewById(R.id.container_layout_of_views);
    }

    private void collapseEditText(int width) {
        ValueAnimator anim = ValueAnimator.ofInt(autoCompletLinearLayout.getWidth(), width);
        Log.i("WIDTH U COLLAPSE METODI", width + "");
        anim.setDuration(FULL_ANIMATION_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = autoCompletLinearLayout.getLayoutParams();
                layoutParams.width = val;
                autoCompletLinearLayout.setLayoutParams(layoutParams);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(autoCompleteTextView.getApplicationWindowToken(), 0);
                autoCompleteTextView.setShowSoftInputOnFocus(false);
            }
        });
        Log.i("WIDTH in collapse", "W=" + width);
        anim.start();
        AnimatorSet animator1 = (AnimatorSet) AnimatorInflater.loadAnimator(MainActivity.this, R.animator.flip_and_change_search_icon_to_gps);
        animator1.setTarget(searchButton);
        animator1.start();

        searchButton.postDelayed(() -> {
            searchButton.setBackground(ContextCompat.getDrawable(MainActivity.this,
                    R.drawable.baseline_search_24));
            searchButton.setTag("search");
            Log.i("POREDJENJE", "SEARCH" + " " + searchButton.getTag());

//            checkBackgroundOfSearchButton();
        }, HALF_ANIMATION_DURATION);

        //Cisti autoCompleteTextView nakon sto korisnik odabere opciju
        autoCompleteTextView.postDelayed(() -> {
            autoCompleteTextView.getText().clear();
        }, FULL_ANIMATION_DURATION);
    }


    private void expandEditText(int width, int targetWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(width
                , targetWidth);
        animator.setDuration(FULL_ANIMATION_DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                int val = (int) animator.getAnimatedValue();
                ViewGroup.LayoutParams params = autoCompletLinearLayout.getLayoutParams();
                params.width = val;
                autoCompletLinearLayout.setLayoutParams(params);
            }
        });
        // If autoCompleteTextView gains focus, start the animation and show the keyboard after a delay
        animator.start();
        //Animacija kojom se searchButton rotira oko Y ose i smanjuje mu se alpha vrednost
        AnimatorSet animator1 = (AnimatorSet) AnimatorInflater.loadAnimator(MainActivity.this, R.animator.flip_and_change_search_icon_to_gps);
        animator1.setTarget(searchButton);
        animator1.start();
        //Nakon 500 ms kada krene animacija menja se ikona searchButtona u GPS ikonu, dakle na pola trajanja animacije
        searchButton.postDelayed(() -> {
            searchButton.setBackground(ContextCompat.getDrawable(MainActivity.this,
                    R.drawable.baseline_my_location_24));
            searchButton.setTag("gps");
            Log.i("POREDJENJE", "MY LOCATION" + " " + searchButton.getTag());
        }, HALF_ANIMATION_DURATION);
        //Nakon 1 sekunde tastatura iskace u auto complete text view-u
        autoCompleteTextView.postDelayed(() -> {
            Log.i("POST", "delayed unutar on focus change listener");
            //Omogucuje da se tastatura pojavi nakon 1000ms
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(autoCompleteTextView, InputMethodManager.SHOW_IMPLICIT);
            //Vraca show soft input koji omogucava da korisnik ponovo moze da ...
            //...selektovanjem autoCompletETextView-a omoguci pojavu tastature
            autoCompleteTextView.setShowSoftInputOnFocus(true);
        }, FULL_ANIMATION_DURATION);
    }

    private void checkBackgroundOfSearchButton() {
        if (!isInternetAvailable()) {
            Toast.makeText(this, R.string.toast_please_turn_on_internet, Toast.LENGTH_SHORT).show();
        } else if (isSearchButtonBackgroundGpsIcon()) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Location permission denied. To use this feature, go to app settings and enable location permission.", Toast.LENGTH_LONG).show();
                showDialogWhenUserTapOnGpsButton();
            }else{
                getLocation();
                checkInternetConnection();
            }
        }
    }

    private void openAppSettings(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",getPackageName(),null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void showDialogWhenUserTapOnGpsButton(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.location_denied_gps_icon_click_toast)
                .setPositiveButton(getString(R.string.go_to_settings), (dialog, which) -> {
                    openAppSettings();
                })
                .setNegativeButton(getString(R.string.cancel_dialog), (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    //Postavljamo swipeRefrestLAyout koji obuhvata sve viewe ispod APP bara na visible kada korisnik ukuca
    //mesto u autoCompleteTextView-u nakon prvog pokretanja app ako odbije dozvolu za lokaciju
    private void setViewsVisible() {
        if (swipeRefreshLayout.getVisibility() == View.GONE) {
            swipeRefreshLayout.setVisibility(View.VISIBLE);
        }
    }


    private boolean isSearchButtonBackgroundGpsIcon() {
        return searchButton.getTag() == "gps";
    }

    private void checkInternetConnection() {
        if (!isInternetAvailable() && !preferences.contains("temperature")) {
            containerLayout.setVisibility(View.GONE);
            noInternet.setVisibility(View.VISIBLE);
        } else {
            containerLayout.setVisibility(View.VISIBLE);
            noInternet.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Toast.makeText(this, "Latitude getLocationChange" + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String nameOfCity = "";
            if (addresses.get(0).getLocality() != null) {
                nameOfCity = addresses.get(0).getLocality();
            } else if (addresses.get(0).getSubAdminArea() != null) {
                nameOfCity = addresses.get(0).getSubAdminArea() + ", " + addresses.get(0).getAdminArea();
            } else if (addresses.get(0).getAdminArea() != null) {
                nameOfCity = addresses.get(0).getAdminArea();
            }
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            editor.putFloat("longitude", (float) longitude);
            editor.putFloat("latitude", (float) latitude);
            editor.putString("city", nameOfCity);
            editor.apply();
            viewModel.doBackgroundTask(latitude, longitude);
            cityName.setText(nameOfCity);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                            , Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST_CODE);
        }
    }



    private void getLocation() {
        Log.i("VEZBANJE", "Get Location Metoda");
        try {
//            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void stPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.i("VEZBANJE", "Get Location Metoda IF");

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                                , Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_REQUEST_CODE);
                swipeRefreshLayout.setVisibility(View.GONE);

            } else {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (location != null) {
                    Log.i("VEZBANJE", "Get Location Metoda Else " + location.getLatitude() + " " + location.getLongitude());
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Toast.makeText(MainActivity.this, "Longituda lokacije : " +
                            location.getLongitude(), Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("VEZBANJE", "last knwon location is NULL");
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permissions granted, proceed with your location-related code
                // ...
                getLocation();
                Log.i("DOZVOLA"," broj grant results: "  + grantResults.length + "0," + grantResults[0] + " permissions: " + permissions.length +" " + permissions[0]);

                Toast.makeText(this, "LOKACIJA OK", Toast.LENGTH_SHORT).show();
            } else {
                // Location permissions denied, show an error message or disable location-related functionality
                // ...
//                Toast.makeText(this, "Please grant location permission to use this feature", Toast.LENGTH_SHORT).show();
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    showLocationDialog();
                }else{

                }

                autoCompletLinearLayout.performClick(); //Mora ovo jer je autoComplteteTextView.onFocusChange inside this

                if (autoCompleteTextView.hasFocus()) {
                    Log.i("FOKUS", "IMA FOKUS");

                } else {
                    Log.i("FOKUS", "NEMA FOKUS");
                }
            }
        }

    }


    private void showLocationDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.permission_dialog_title))
                .setMessage(getString(R.string.permission_dialog_message))
                .setPositiveButton(getString(R.string.grant_permission), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_REQUEST_CODE);
                    }
                })
                .setNegativeButton(getString(R.string.cancel_permission), null).show();
    }


    @Override
    public void onProviderEnabled(@NonNull String provider) {
        loadSharedPreferences();    //onProviderEnabled metoda
        Log.i("VEZBANJE", "onProviderEnabled, lat i lon: " + latitude + " " + longitude);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.i("VEZBANJE", "onProviderDisabled");
        Toast.makeText(this, "GPS NIJE DOSTUPAN", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.gps_not_available_dialog_message))
                .setPositiveButton(getString(R.string.turn_on_gps), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.cancel_dialog), null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void storeCityName() {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        List<Address> addresses = null;
        String nameOfCity = "";
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);   //storeCityName Metoda
            if (addresses.get(0).getLocality() != null) {
                nameOfCity = addresses.get(0).getLocality();
            } else if (addresses.get(0).getSubAdminArea() != null) {
                nameOfCity = addresses.get(0).getSubAdminArea() + ", " + addresses.get(0).getAdminArea();
            } else if (addresses.get(0).getAdminArea() != null) {
                nameOfCity = addresses.get(0).getAdminArea();
            }
            Log.i("ADRESS GEOCODER:", addresses.get(0).getLocality() + "- LOCALITY\n" +
                    addresses.get(0).getCountryName() + "-COUNTRY NAME\n" +
                    addresses.get(0).getAdminArea() + "-ADMIN AREA" + "\n" +
                    addresses.get(0).getSubAdminArea() + " " + addresses.get(0).getSubLocality() + " Sub admin area and sub locality" + "\n" +
                    addresses.get(0).getFeatureName() + "-FEATURE NAME" + "\n" +
                    addresses.get(0).getSubThoroughfare() + "-SUB THOROUGHFARE|\n" +
                    addresses.get(0).getPremises() + "-PREMISSES");

            Log.i("SWIPE", latitude + " JE LAT U STORECITY");
        } catch (IOException e) {
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
        }
        editor.putString("city", nameOfCity);
        editor.apply();

        cityName.setText(nameOfCity);
        viewModel.doBackgroundTask(latitude, longitude);    //storeCityName viewmodel

    }

    private void loadSharedPreferences() {
        String city = preferences.getString("city", "");
        int temperature = preferences.getInt("temperature", 0);
        float longitude = preferences.getFloat("longitude", 0.0f);
        float latitude = preferences.getFloat("latitude", 0.0f);
        int prefPrecipitation = preferences.getInt("precipitation", 0);
        int prefMinDayTemp = preferences.getInt("min_day_temp", 0);
        int prefMaxDayTemp = preferences.getInt("max_day_temp", 0);
        int prefHumidity = preferences.getInt("humidity", 0);
        String prefWindDirection = findWindDirection(preferences.getFloat("wind_direction", 0.0f));
        int prefWeatherCode = preferences.getInt("weather_code", 0);
        String prefTime = preferences.getString("time", "");
        boolean isDay = preferences.getBoolean("is_day", true);
        if (!city.isEmpty()) {
            cityName.setText(city);
            this.temperature.setText(convertTemperature(temperature) + "");
            windData.setText(setSpeedWind(preferences.getInt("wind_speed", 0)) +
                    prefWindDirection);
            this.longitude = longitude;
            this.latitude = latitude;   //load shared preferences metoda
            precipitationData.setText(prefPrecipitation + "%");
            maxMinTemperature.setText(weatherDescription + " " + convertTemperature(prefMaxDayTemp) + "° / " +
                    convertTemperature(prefMinDayTemp) + "°");
            humidityData.setText(prefHumidity + "%");
            dateAndTime.setText(localizedDay() + " " + prefTime);
            checkWeatherCode(prefWeatherCode, isDay);
            Log.i("Vezbanje", "PRef weather code uload shared pref:" + prefWeatherCode);
        }
    }


    //Find Wind Direction ( Pravac vetra je dat u stepenima, na osnovu kojih odredjujemo smer)
    private String findWindDirection(double windDirection) {
        String direction = "";
        if (windDirection >= 337.5 || windDirection <= 22.5) direction = getString(R.string.north);
        else if (windDirection <= 67.5) direction = getString(R.string.north_east);
        else if (windDirection <= 112.5) direction = getString(R.string.east);
        else if (windDirection <= 157.5) direction = getString(R.string.south_east);
        else if (windDirection < 202.5) direction = getString(R.string.south);
        else if (windDirection < 247.5) direction = getString(R.string.south_west);
        else if (windDirection < 292.5) direction = getString(R.string.west);
        else if (windDirection < 337.5) direction = getString(R.string.north_west);
        return direction;
    }

    private void checkWeatherCode(int weatherCode, boolean isDay) {
        switch (weatherCode) {
            case 0:
                if (isDay) {
                    setImagesForWeatherCode(R.drawable.weather_code_0_sun_, true, isDay, getString(R.string.clear_sky));
                } else {
                    setImagesForWeatherCode(R.drawable.clear_sky_night_moon, true, isDay, getString(R.string.clear_sky));
                }
                break;
            case 1:
            case 2:
                if (isDay) {
                    setImagesForWeatherCode(R.drawable.weather_code_1_partly_cloudy, true, isDay, getString(R.string.partially_cloudy));
                } else {
                    setImagesForWeatherCode(R.drawable.night_moon_partly_cloud, true, isDay, getString(R.string.partially_cloudy));
                }
                break;
            case 3:
                if (getCurrentSeason().equals("Leto")) {
                    if (isDay) {
                        setImagesForWeatherCode(R.drawable.weather_code_1_partly_cloudy, true, isDay, getString(R.string.partially_cloudy));
                    } else {
                        setImagesForWeatherCode(R.drawable.night_moon_partly_cloud, true, isDay, getString(R.string.partially_cloudy));
                    }
                } else {
                    setImagesForWeatherCode(R.drawable.overcast_3_cloudy, false, isDay, getString(R.string.cloudy));
                }
                break;
            case 45:
            case 48:
                setImagesForWeatherCode(R.drawable.weather_code_4_fog, false, isDay, getString(R.string.fog));
                break;
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
                setImagesForWeatherCode(R.drawable.weather_code_6_rain, false, isDay, getString(R.string.rain));
                break;
            case 80:
            case 81:
            case 82:
            case 85:
            case 86:
                setImagesForWeatherCode(R.drawable.weather_code_8_showers, false, isDay, getString(R.string.shower));
                break;
            case 71:
            case 73:
            case 75:
            case 77:
                setImagesForWeatherCode(R.drawable.weather_code_7_snow, false, isDay, getString(R.string.snow));
                break;
            case 95:
            case 96:
            case 99:
                setImagesForWeatherCode(R.drawable.weather_code_9_thunder, false, isDay, getString(R.string.thunderstorm));
                break;
        }
    }


    @Override
    public void onBackPressed() {
        if (autoCompleteTextView.hasFocus()) {
            autoCompleteTextView.clearFocus();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i("TWICE","on rESUME");

        //PRoveravamo da li autoCompleteTextView ima fokus jer zelimo da se animacija vrati na pocetak kada se metoda onResume izvrsi
        //Npr kada autoCompleteTV ima fokus i idemo u settings activity i vratimo se nazad da animacija vrati textView na pocetni polozaj
        if (autoCompleteTextView.hasFocus()) {
            autoCompleteTextView.clearFocus();
        }
        if (preferences.getAll().isEmpty() || !preferences.contains("city")) {
//            getLocation();
            swipeRefreshLayout.setVisibility(View.GONE);

        } else {
            loadSharedPreferences(); //onResume
            latitude = preferences.getFloat("latitude", 0.0f);     //onResume metoda
            longitude = preferences.getFloat("longitude", 0.0f);
            Log.i("VEZBANJE", "RESUME lat long pre view modela: " + latitude + " " + longitude);

            viewModel.doBackgroundTask(latitude, longitude);    //onResume viewmodel
            Log.i("VEZBANJE", "RESUME lat long posle view modela: " + latitude + " " + longitude);
        }
    }

    /**
     * Metoda za postavljanje datuma i vremena ispod naziva grada u formatu:
     * Naziv dana u nedelji sat : minuti
     */
    private void setDateAndTime() {
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        Locale locale;
        String languageTag = getResources().getString(R.string.locale);
        locale = new Locale(languageTag);
        String day = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        String time = formatter.format(dateTime);
        String dayCapitalizeFirst = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase(Locale.ROOT);
        dateAndTime.setText(dayCapitalizeFirst + " " + time);
        editor.putString("date", dayCapitalizeFirst);
        editor.putString("time", time);
        editor.apply();
    }

    private String localizedDay() {
        LocalDateTime dateTime = LocalDateTime.now();
        Locale locale;
        String languageTag = getResources().getString(R.string.locale);
        locale = new Locale(languageTag);
        String day = dateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        return day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase(Locale.ROOT);
    }

    private void swipeAndRefresh() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("SWIPE", latitude + " LATITUDA");
                checkInternetConnection();

                latitude = preferences.getFloat("latitude", 0.0f);
                longitude = preferences.getFloat("longitude", 0.0f);
                viewModel.doBackgroundTask(latitude, longitude);   //swipe and refresh viewmodel
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void setLoadWeatherForecastPreference(List<WeatherForecast> forecasts) {
        for (int i = 0; i < 5; i++) {
            recreateViewsEditor.putString("day_name" + i, forecasts.get(i).getDayName());
            recreateViewsEditor.putInt("max_temp" + i, forecasts.get(i).getMaxTemperature());
            recreateViewsEditor.putInt("weather_code" + i, forecasts.get(i).getWeatherCode());
            recreateViewsEditor.putInt("icon_resource" + i, iconResourcId);
            recreateViewsEditor.putInt("min_temp" + i, forecasts.get(i).getMinTemperature());
            recreateViewsEditor.apply();
        }
    }

    //Proveravamo godisnje doba jer Weather API koji koristimo ne daje realni weather code za prognozu za 5 dana
    //Ako je leto umesto overcast slike stavljamo delimicno oblacno
    public static String getCurrentSeason() {
        LocalDate localDate = LocalDate.now();

        int month = localDate.getMonthValue();
        int day = localDate.getDayOfMonth();
        if (month == 4 || month == 5 || (month == 3 && day >= 20) || (month == 6 && day <= 19))
            return "Prolece";
        else if (month == 7 || month == 8 || (month == 6 && day >= 21) || (month == 9 && day < 22))
            return "Leto";
        else if (month == 10 || month == 11 || (month == 9 && day >= 23) || (month == 12 && day < 21))
            return "Jesen";
        else return "Zima";
    }


    /**
     * Pomocna metoda kojom se postavljaju slike i pozadine
     *
     * @param weatherImageResource   - integer vrednost koja predstavlja sliku koju je neophodno postaviti
     *                               na weatherImage(Image View) u zavisnosti od vremenskih uslova
     * @param isClearSky                - boolean vrednost koji proverava da li je vreme suncano ili ne i u zavisnosti
     *                               od toga postavlja pozadinu aplikacije, pozadinu transparentnih view-a(vetar,
     *                               mogucnost padavina, vlaznost i root view u kojem se nalazi prognoza za narednih 5 dana)
     * @param nameOfWeatherCondition - String vrednost kojom opisijuemo trenutne vremenske uslove
     */
    private void setImagesForWeatherCode(int weatherImageResource, boolean isClearSky, boolean isDay, String nameOfWeatherCondition) {
        weatherImage.setImageResource(weatherImageResource);
        if (isDay) {
            if (isClearSky) {
                setWeatherImagesHelper(R.drawable.background_clear_sky,
                        R.drawable.weather_condition_background_dark,
                        R.drawable.weather_condition_background_dark,
                        R.drawable.weather_condition_background_dark,
                        R.drawable.weather_condition_background_dark);
            } else {
                setWeatherImagesHelper(R.drawable.background_thunder_sky,
                        R.drawable.weather_condition_background,
                        R.drawable.weather_condition_background,
                        R.drawable.weather_condition_background,
                        R.drawable.weather_condition_background);
            }
        } else {
            setWeatherImagesHelper(
                    R.drawable.background_night_sky,            //Root view background
                    R.drawable.weather_condition_background,    //Forecast background view
                    R.drawable.weather_condition_background,    //Wind background view
                    R.drawable.weather_condition_background,    //Precipitation bg view
                    R.drawable.weather_condition_background);   //humidity bg view
        }
        weatherDescription = nameOfWeatherCondition;
        iconResourcId = weatherImageResource;
    }

    private void setWeatherImagesHelper(int rootViewResourceId, int forecastRootResourceId,
                                        int windLinearLayoutId, int precipitationLinearLayId, int humidityLinearLayoutResourceId) {
        rootView.setBackground(ContextCompat.getDrawable(this, rootViewResourceId));
        forecastRoot.setBackground(ContextCompat.getDrawable(this, forecastRootResourceId));
        windLinearLayout.setBackground(ContextCompat.getDrawable(this, windLinearLayoutId));
        precipitationLinearLay.setBackground(ContextCompat.getDrawable(this, precipitationLinearLayId));
        humidityLinearLay.setBackground(ContextCompat.getDrawable(this, humidityLinearLayoutResourceId));
    }
    private void openSettingActivity() {
        settings.setOnClickListener(e -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    private void showToolTipText() {
        windLinearLayout.setTooltipText(getString(R.string.wind));
        precipitationLinearLay.setTooltipText(getString(R.string.precipitation));
        humidityLinearLay.setTooltipText(getString(R.string.humidity));
    }


    private void loadForecastPreferencestForNextFiveDay() {
            forecastRoot.removeAllViews();  //Kada menjamo lokaciju prvo uklanjamo sve viewe koje forecastRoot sadrzi pa kreiramo nove sa novim vrednostima
            for (int i = 0; i < 5; i++) {
                LinearLayout dayLayout = new LinearLayout(this);
                TextView dayName = new TextView(this);
                LinearLayout.LayoutParams dayNameParams;
                TextView max = new TextView(this);
                LinearLayout.LayoutParams maxTempParams;
                ImageView icon = new ImageView(this);
                if (getResources().getBoolean(R.bool.is_tablet_landscape)) {
                    LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
                    dayLayout.setLayoutParams(linearLayoutParams);
                    dayLayout.setOrientation(LinearLayout.HORIZONTAL);
                    dayLayout.setGravity(Gravity.CENTER_VERTICAL);
                    dayNameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f);
                    dayName.setGravity(Gravity.START);
                    maxTempParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    int iconWidthAndHEight = 30; //Koju velicinu zelimo u DP, jer params prima samo brojeve u pikslima
                    int desiredWidthAndHeightInPx = (int) (iconWidthAndHEight * getResources().getDisplayMetrics().density); //Koriscenjem device screen density se dp konvertuje u px
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(desiredWidthAndHeightInPx, desiredWidthAndHeightInPx);
                    icon.setLayoutParams(iconParams);
                } else {
                    LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    dayLayout.setLayoutParams(linearLayoutParams);
                    dayLayout.setOrientation(LinearLayout.VERTICAL);
                    dayLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                    dayNameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 0.8f);
                    if (heightDP < 730) {     //AKO JE VELICINA EKRANA MANJA od 730 dp onda stavljamo da velicina teksta za dane bude 8 sp
                        dayName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
                    } else {
                        dayName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                    }
                    dayName.setGravity(Gravity.CENTER);
                    maxTempParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1.0f);
                    int iconWidthAndHEight = 30; //Koju velicinu zelimo u DP, jer params prima samo brojeve u pikslima
                    int desiredWidthAndHeightInPx = (int) (iconWidthAndHEight * getResources().getDisplayMetrics().density); //Koriscenjem device screen density se dp konvertuje u px
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(desiredWidthAndHeightInPx, desiredWidthAndHeightInPx);
                    icon.setLayoutParams(iconParams);
                }
                dayName.setLayoutParams(dayNameParams);
                dayName.setTypeface(null, Typeface.BOLD);
                dayName.setText(changeNameOfDayInForecast(recreateViewsPreferences.getString("day_name" + i, "")).toUpperCase()); //weatherForecast.getDayName()
                max.setLayoutParams(maxTempParams);
                max.setGravity(Gravity.CENTER);
                max.setText(convertTemperature(recreateViewsPreferences.getInt("max_temp" + i, 0)) + "°");//weatherForecast.getMaxTemperature() + "°"
                checkWeatherCode(recreateViewsPreferences.getInt("weather_code" + i, 0), true);//Is day =true jer prikazujemo slike koje se odnose za dan(sunce a ne mesec)
                icon.setImageResource(iconResourcId);
                TextView min = new TextView(this);
                min.setLayoutParams(maxTempParams);
                min.setGravity(Gravity.CENTER);
                min.setText(convertTemperature(recreateViewsPreferences.getInt("min_temp" + i, 0)) + "°");
                if (getResources().getBoolean(R.bool.is_tablet_landscape)) {
                    dayLayout.addView(dayName);
                    dayLayout.addView(min);
                    dayLayout.addView(icon);
                    dayLayout.addView(max);
                } else {
                    dayLayout.addView(dayName);
                    dayLayout.addView(max);
                    dayLayout.addView(icon);
                    dayLayout.addView(min);
                }
                forecastRoot.addView(dayLayout);
            }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }


    /**
     * Temperatura je data u celzijusima u okviru API-a. Kada je preference-u u SettingsActivity selektovan F
     * kao jedinica za temperaturu, vrsimo konverziju iz Celzijusa u Farenhajte. Ako nije selektovana samo menjamo
     * temperatureUnit(TextView) da prikazuje °C umesto °F
     *
     * @param temp Temperatura  koju konvertujemo.
     * @return converted temperature
     */
    private int convertTemperature(int temp) {
        String tempUnit = settingsPreferences.getString("temperature_unit", "celsius");
        if (tempUnit.equals("fahrenheit")) {
            temp = convertFromCelsiusToFahrenheit(temp);
            temperatureUnit.setText("°F");
        } else {
            temperatureUnit.setText("°C");
        }
        return temp;
    }


    private int convertFromCelsiusToFahrenheit(int celsius) {
        return (int) Math.round((celsius * 9.0 / 5) + 32);
    }

    private String setSpeedWind(int windSpeed) {
        String windUnit = settingsPreferences.getString("wind_speed_unit", "km_per_hour");

        if (windUnit.equals("meter_per_second")) {
            windSpeed = convertWindFromKmPerHourToMPerSecond(windSpeed);
            windUnitName = " m/s ";
        } else if (windUnit.equals("miles_per_hour")) {
            windSpeed = convertWindFromKmPerHourtoMilesPerHour(windSpeed);
            windUnitName = " mps ";
        } else if (windUnit.equals("km_per_hour")) {
            windUnitName = " km/h ";
        }
        return windSpeed + windUnitName;
    }

    private int convertWindFromKmPerHourToMPerSecond(int windSpeed) {
        return (int) Math.round(windSpeed * (5.0 / 18));//5/18 je skraceno od 1000/3600 (km/h u m/s)
    }

    private int convertWindFromKmPerHourtoMilesPerHour(int windSpeed) {
        return (int) Math.round(windSpeed / 1.609); //brzinu u km/h delimo sa 1.609(jedna milja sadrzi tolko km)
    }

    private void changeLanguage() {
        String languagePref = settingsPreferences.getString("language", "Srpski"); //english
        if(getLanguageTag(languagePref).equals(getLanguageTag("Srpski"))){
                setLocale("sr-Latn");
        }else{
            setLocale("en");
        }
    }
    private String getLanguageTag(String language){
        return language.equals("English") ? "en" : "sr-Latn";
    }
    private boolean isCurrentLanguageEnglish() {
        String language = settingsPreferences.getString("language", "Srpski");
        Log.i("LANGUAGE", language);
        return language.equals("English");
    }
    private void setLocale(String language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }
    private String changeNameOfDayInForecast(String dayName) {
        if (isCurrentLanguageEnglish() && !isCurrentDayNameEnglish(dayName)) {
            for (Map.Entry<String, String> days : daysNameHashMap().entrySet()) {
                if (dayName.equalsIgnoreCase(days.getValue())) {
                    dayName = days.getKey();
                }
            }
        } else
            if (!isCurrentLanguageEnglish() && isCurrentDayNameEnglish(dayName))
        {
            for (Map.Entry<String, String> days : daysNameHashMap().entrySet()) {
                if (dayName.equalsIgnoreCase(days.getKey())) {
                    dayName = days.getValue();
                    break;
                }
            }
        }
        return dayName;
    }
    private boolean isCurrentDayNameEnglish(String dayName) {
        dayName = dayName.toLowerCase();
        switch (dayName) {
            case "monday":
            case "tuesday":
            case "wednesday":
            case "thursday":
            case "friday":
            case "saturday":
            case "sunday":
                return true;
        }
        return false;
    }
    private HashMap<String, String> daysNameHashMap() {
        HashMap<String, String> days = new HashMap<>();
        days.put("Monday", "Ponedeljak");
        days.put("Tuesday", "Utorak");
        days.put("Wednesday", "Sreda");
        days.put("Thursday", "Četvrtak");
        days.put("Friday", "Petak");
        days.put("Saturday", "Subota");
        days.put("Sunday", "Nedelja");
        return days;
    }

}