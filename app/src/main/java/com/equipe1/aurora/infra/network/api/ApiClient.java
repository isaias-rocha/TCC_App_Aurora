package com.equipe1.aurora.infra.network.api;

// =========================================================================
// 1. IMPORTAÇÕES DE DEPENDÊNCIAS
// =========================================================================

// Classes do próprio projeto (Adaptador customizado de lista de GeoPoint)

// Biblioteca Gson (Conversão e serialização/desserialização de JSON)
import com.equipe1.aurora.infra.network.GeoPointListDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

// Biblioteca OSMDroid (Representação de coordenadas geográficas)
import org.osmdroid.util.GeoPoint;

// Utilitários nativos do Java (Reflexão e manipulação de Listas)
import java.lang.reflect.Type;
import java.util.List;

// Biblioteca Retrofit (Cliente HTTP para requisições de API REST)
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * =========================================================================
 * 2. DEFINIÇÃO DA CLASSE CLIENTE HTTP (SINGLETON)
 * =========================================================================
 * Classe utilitária responsável por centralizar a configuração do Retrofit,
 * do Gson e por prover uma única instância (Singleton) do `ApiService`.
 */
public class ApiClient {

    // =========================================================================
    // 3. CONSTANTES E CONFIGURAÇÕES DE REDE
    // =========================================================================
    // Regra do Retrofit: a URL Base DEVE obrigatoriamente terminar com barra "/".
    // - Para Emulador do Android Studio na mesma máquina: "http://10.0.2.2:8080/"

    // - Para Dispositivo Físico no Wi-Fi local: "http://192.168.1.8/api_aurora_gps/"
    private static final String BASE_URL = "http://192.168.15.4:8080/";


    // =========================================================================
    // 4. ATRIBUTOS ESTÁTICOS DE ESTADO (SINGLETON)
    // =========================================================================
    // Guarda a referência única da interface de serviços para reuso na aplicação
    private static ApiService apiService;

    // =========================================================================
    // 5. CONSTRUTOR PRIVADO
    // =========================================================================
    // Construtor privado previne a criação inadvertida de instâncias via `new ApiClient()`.
    private ApiClient() {
    }

    // =========================================================================
    // 6. MÉTODOS PÚBLICOS DE ACESSO
    // =========================================================================
    /**
     * Obtém ou inicializa a instância única do `ApiService`.
     * Aplica o padrão Lazy Initialization (instanciação sob demanda).
     *
     * @return Instância pronta para executar as chamadas HTTP declaradas no ApiService.
     */
    public static ApiService getApiService() {
        if (apiService == null) {

            // -----------------------------------------------------------------
            // PASSO 1: Mapeamento do Tipo Genérico e Registro do Adaptador Gson
            // -----------------------------------------------------------------
            // Devido ao "Type Erasure" do Java, tipos genéricos como List<GeoPoint>
            // perdem a informação do objeto contido em tempo de execução.
            // O `TypeToken` preserva a estrutura List<GeoPoint> completa para o Gson.
            Type listGeoPointType = new TypeToken<List<GeoPoint>>() {}.getType();

            // Registra o desserializador customizado para listas de coordenadas geográficas
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(listGeoPointType, new GeoPointListDeserializer())
                    .create();

            // -----------------------------------------------------------------
            // PASSO 2: Construção da Instância do Retrofit
            // -----------------------------------------------------------------
            // Configura a URL base e anexa o conversor Gson com os adaptadores registrados
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            // -----------------------------------------------------------------
            // PASSO 3: Geração Dinâmica da Interface de API
            // -----------------------------------------------------------------
            // O Retrofit gera o código de implementação da interface ApiService em tempo de execução
            apiService = retrofit.create(ApiService.class);
        }

        // Retorna a instância ativa (seja recém-criada ou reaproveitada)
        return apiService;
    }
}