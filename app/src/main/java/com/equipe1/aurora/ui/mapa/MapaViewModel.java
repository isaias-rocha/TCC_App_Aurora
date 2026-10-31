package com.equipe1.aurora.ui.mapa;

// ============================================================================================
// ARQUIVO: MapaViewModel.java
// CAMADA: Apresentação (ViewModel - MVVM)
// FUNÇÃO: Gerencia e encapsula os estados da interface do mapa (pontos selecionados, dispositivo
//         ESP32, navegação) e orquestra a comunicação assíncrona via Retrofit com os serviços de
//         camadas de segurança (áreas de risco, bairros e crimes), reexpondo os resultados como
//         LiveData para a View.
// ============================================================================================

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.equipe1.aurora.domain.model.AreaRisco;
import com.equipe1.aurora.domain.model.BairroPoligono;
import com.equipe1.aurora.domain.model.Circulo;
import com.equipe1.aurora.domain.model.OcorrenciaCrime;
import com.equipe1.aurora.infra.network.api.ApiClient;
import com.equipe1.aurora.infra.network.api.ApiService;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaViewModel extends ViewModel {

    // =========================================================================
    // 1. ESTADOS DO MAPA, NAVEGAÇÃO E ESP32 (LIVEDATA)
    // =========================================================================

    // Armazena o ponto selecionado manualmente pelo usuário no mapa
    private final MutableLiveData<GeoPoint> pontoSelecionado = new MutableLiveData<>();

    // Armazena a localização obtida pelo hardware ESP32
    private final MutableLiveData<GeoPoint> pontoEsp32 = new MutableLiveData<>();

    // Lista de círculos de contatos
    private final MutableLiveData<List<Circulo>> listaCirculos = new MutableLiveData<>(new ArrayList<>());

    // Controla se a navegação por GPS está ativa
    private final MutableLiveData<Boolean> estaNavegando = new MutableLiveData<>(false);

    // =========================================================================
    // 2. ESTADOS DAS CAMADAS DE SEGURANÇA E RISCO (LIVEDATA)
    // =========================================================================

    // Lista de áreas de risco recebidas da API
    private final MutableLiveData<List<AreaRisco>> listaRiscos = new MutableLiveData<>(new ArrayList<>());

    // Lista de polígonos dos bairros
    private final MutableLiveData<List<BairroPoligono>> listaBairros = new MutableLiveData<>(new ArrayList<>());

    // Lista de ocorrências criminais
    private final MutableLiveData<List<OcorrenciaCrime>> listaCrimes = new MutableLiveData<>(new ArrayList<>());

    // Flag de visibilidade das camadas de risco
    private final MutableLiveData<Boolean> exibirAreasRisco = new MutableLiveData<>(true);

    // Mensagens de erro de comunicação
    private final MutableLiveData<String> mensagemErro = new MutableLiveData<>();

    // =========================================================================
    // 3. GETTERS E SETTERS DAS VARIÁVEIS DE ESTADO
    // =========================================================================

    public LiveData<GeoPoint> getPontoSelecionado() { return pontoSelecionado; }
    public void setPontoSelecionado(GeoPoint ponto) { pontoSelecionado.setValue(ponto); }

    public LiveData<GeoPoint> getPontoEsp32() { return pontoEsp32; }
    public void setPontoEsp32(GeoPoint ponto) { pontoEsp32.setValue(ponto); }

    public LiveData<List<Circulo>> getListaCirculos() { return listaCirculos; }
    public void setListaCirculos(List<Circulo> circulos) { listaCirculos.setValue(circulos); }

    public LiveData<Boolean> getEstaNavegando() { return estaNavegando; }
    public void setEstaNavegando(boolean navegando) { estaNavegando.setValue(navegando); }

    public LiveData<List<AreaRisco>> getListaRiscos() { return listaRiscos; }
    public LiveData<List<BairroPoligono>> getListaBairros() { return listaBairros; }
    public LiveData<List<OcorrenciaCrime>> getListaCrimes() { return listaCrimes; }

    public LiveData<Boolean> getExibirAreasRisco() { return exibirAreasRisco; }
    public void alternarExibicaoRiscos() {
        Boolean atual = exibirAreasRisco.getValue();
        exibirAreasRisco.setValue(atual == null || !atual);
    }

    public LiveData<String> getMensagemErro() { return mensagemErro; }

    // =========================================================================
    // 4. REQUISIÇÕES DA CAMADA DE DADOS (RETROFIT)
    // =========================================================================

    // Dispara a busca simultânea de todas as camadas de risco na API
    public void carregarTodasAsCamadasDeRisco() {
        carregarAreasDeRisco();
        carregarPoligonosBairros();
        carregarOcorrenciasCrimes();
    }

    // Requisição assíncrona para obter áreas de risco
    private void carregarAreasDeRisco() {
        ApiService apiService = ApiClient.getApiService();
        apiService.listarRiscos().enqueue(new Callback<List<AreaRisco>>() {
            @Override
            public void onResponse(Call<List<AreaRisco>> call, Response<List<AreaRisco>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaRiscos.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<AreaRisco>> call, Throwable t) {
                Log.e("MAPA_VM_ERRO", "Erro ao carregar áreas de risco: " + t.getMessage());
            }
        });
    }

    // Requisição assíncrona para obter polígonos de bairros
    private void carregarPoligonosBairros() {
        ApiService apiService = ApiClient.getApiService();
        apiService.listarBairros().enqueue(new Callback<List<BairroPoligono>>() {
            @Override
            public void onResponse(Call<List<BairroPoligono>> call, Response<List<BairroPoligono>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaBairros.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<BairroPoligono>> call, Throwable t) {
                Log.e("MAPA_VM_ERRO", "Erro ao carregar polígonos de bairros: " + t.getMessage());
            }
        });
    }

    // Requisição assíncrona para obter ocorrências de crimes
    private void carregarOcorrenciasCrimes() {
        ApiService apiService = ApiClient.getApiService();
        apiService.listarCrimes().enqueue(new Callback<List<OcorrenciaCrime>>() {
            @Override
            public void onResponse(Call<List<OcorrenciaCrime>> call, Response<List<OcorrenciaCrime>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listaCrimes.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<OcorrenciaCrime>> call, Throwable t) {
                Log.e("MAPA_VM_ERRO", "Erro ao carregar ocorrências de crimes: " + t.getMessage());
            }
        });
    }
}