package com.equipe1.aurora.ui.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.equipe1.aurora.domain.model.Circulo;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsável por gerenciar e manter o estado da interface do mapa.
 * Preserva dados na memória durante mudanças de configuração (ex: rotação da tela).
 */
public class MapaViewModel extends ViewModel {

    // Armazena o ponto/destino selecionado no mapa
    private final MutableLiveData<GeoPoint> pontoSelecionado = new MutableLiveData<>();

    // Armazena a lista de círculos de contatos
    private final MutableLiveData<List<Circulo>> listaCirculos = new MutableLiveData<>(new ArrayList<>());

    // Controla se o modo de navegação ativa está ligado
    private final MutableLiveData<Boolean> estaNavegando = new MutableLiveData<>(false);

    // --- GETTERS E SETTERS (LIVEDATA) ---

    public LiveData<GeoPoint> getPontoSelecionado() {
        return pontoSelecionado;
    }

    public void setPontoSelecionado(GeoPoint ponto) {
        pontoSelecionado.setValue(ponto);
    }

    public LiveData<List<Circulo>> getListaCirculos() {
        return listaCirculos;
    }

    public void setListaCirculos(List<Circulo> circulos) {
        listaCirculos.setValue(circulos);
    }

    public LiveData<Boolean> getEstaNavegando() {
        return estaNavegando;
    }

    public void setEstaNavegando(boolean navegando) {
        estaNavegando.setValue(navegando);
    }
}