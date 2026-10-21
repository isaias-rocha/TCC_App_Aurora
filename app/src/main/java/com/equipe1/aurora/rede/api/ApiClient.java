package com.equipe1.aurora.rede.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.util.GeoPoint;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.15.4:8080/";
    private static ApiService apiService;

    public static ApiService getApiService() {
        if (apiService == null) {

            // Define o tipo genérico List<GeoPoint>
            Type listGeoPointType = new TypeToken<List<GeoPoint>>() {}.getType();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(listGeoPointType, new GeoPointListDeserializer())
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}