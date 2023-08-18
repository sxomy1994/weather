package com.milos.weather.api;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.milos.weather.model.City;

import java.util.List;

public class GeoapifyAdapter extends ArrayAdapter<String> implements Filterable {
    List<City> results;
    GeoapifyApi api = new GeoapifyApi();
    public GeoapifyAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    @Override
    public int getCount() {
        return results.size();
    }
    @Nullable
    @Override
    public String getItem(int position) {
        return results.get(position).getCityName();
    }

    public City getCity(int pos){
        return results.get(pos);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if(constraint!=null){
                    results = api.searchCityAutocomplete(constraint.toString());
                    filterResults.values = results;
                    Log.i("VEZBANJE filterResults.value=", filterResults.values + " ");
                    filterResults.count = results.size();
                }
                return filterResults;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results!=null && results.count > 0){
                    notifyDataSetChanged();
                }else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}
