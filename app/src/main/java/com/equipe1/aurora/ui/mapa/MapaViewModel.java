package com.equipe1.aurora.ui.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.equipe1.aurora.domain.model.Circulo;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class MapaViewModel extends ViewModel {

    private final MutableLiveData<GeoPoint> pontoSelecionado = new MutableLiveData<>();
    private final MutableLiveData<GeoPoint> pontoEsp32 = new MutableLiveData<>();
    private final MutableLiveData<List<Circulo>> listaCirculos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> estaNavegando = new MutableLiveData<>(false);

    // --- GETTERS E SETTERS ---

    public LiveData<GeoPoint> getPontoSelecionado() {
        return pontoSelecionado;
    }

    public void setPontoSelecionado(GeoPoint ponto) {
        pontoSelecionado.setValue(ponto);
    }

    // LiveData para o ESP32
    public LiveData<GeoPoint> getPontoEsp32() {
        return pontoEsp32;
    }

    public void setPontoEsp32(GeoPoint ponto) {
        pontoEsp32.setValue(ponto);
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