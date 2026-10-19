package com.equipe1.aurora.infra.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // -----------------------------------------------------------------
    // ENDEREÇOS BASE CONFIGURÁVEIS:
    // - Para Emulador do Android Studio na mesma máquina: "http://10.0.2.2/api_aurora_gps/"
    // - Para Dispositivo Físico no Wi-Fi: "http://192.168.1.5/api_aurora_gps/" (troque pelo seu IPv4)
    // -----------------------------------------------------------------
    private static final String BASE_URL = "http://192.168.1.5/api_aurora_gps/";

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}