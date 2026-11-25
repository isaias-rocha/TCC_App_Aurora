package com.equipe1.aurora.infra.network.api;

// =========================================================================
// 1. IMPORTAÇÕES DE DEPENDÊNCIAS
// =========================================================================
import com.equipe1.aurora.infra.network.GeoPointListDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.util.GeoPoint;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// =========================================================================
// 2. DEFINIÇÃO DA CLASSE CLIENTE HTTP (SINGLETON)
// =========================================================================
// Classe utilitária responsável por centralizar a configuração do Retrofit,
// do Gson e por prover uma única instância (Singleton) do ApiService.
public class ApiClient {

    // =========================================================================
    // 3. CONSTANTES E CONFIGURAÇÕES DE REDE
    // =========================================================================
    // Regra do Retrofit: a URL Base DEVE terminar obrigatoriamente com barra "/".
    // - Para Emulador do Android Studio na mesma máquina: "http://10.0.2.2/db_aurora/"
    // - Para Dispositivo Físico no Wi-Fi local: "http://IP_LOCAL/diretorio/"
    private static final String BASE_URL = "http://192.168.1.7/api_aurora_gps/";

    // =========================================================================
    // 4. ATRIBUTOS ESTÁTICOS DE ESTADO (SINGLETON)
    // =========================================================================
    // Guarda a referência única da interface de serviços para reuso na aplicação
    private static ApiService apiService;

    // =========================================================================
    // 5. CONSTRUTOR PRIVADO
    // =========================================================================
    // Previne a criação de instâncias diretas via "new ApiClient()".
    private ApiClient() {
    }

    // =========================================================================
    // 6. MÉTODOS PÚBLICOS DE ACESSO
    // =========================================================================
    /**
     * Obtém ou inicializa a instância única do ApiService (Lazy Initialization).
     *
     * @return Instância pronta para executar as chamadas HTTP declaradas.
     */
    public static ApiService getApiService() {
        if (apiService == null) {

            // -----------------------------------------------------------------
            // PASSO 1: Mapeamento do Tipo Genérico e Registro do Adaptador Gson
            // -----------------------------------------------------------------
            Type listGeoPointType = new TypeToken<List<GeoPoint>>() {}.getType();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(listGeoPointType, new GeoPointListDeserializer())
                    .create();

            // -----------------------------------------------------------------
            // PASSO 2: Construção da Instância do Retrofit
            // -----------------------------------------------------------------
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            // -----------------------------------------------------------------
            // PASSO 3: Geração Dinâmica da Interface de API
            // -----------------------------------------------------------------
            apiService = retrofit.create(ApiService.class);
        }

        return apiService;
    }
}