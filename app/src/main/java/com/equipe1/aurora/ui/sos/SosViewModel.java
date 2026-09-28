package com.equipe1.aurora.ui.sos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SosViewModel extends ViewModel {
    private final MutableLiveData<Boolean> _alertaEnviadoComSucesso = new MutableLiveData<>();
    public LiveData<Boolean> alertaEnviadoComSucesso = _alertaEnviadoComSucesso;

    /**
     * Executa a lógica de negócios para obter a localização e notificar os contatos de emergência.
     */
    public void enviarAlertaSos() {
        // TODO: Inserir a chamada da API/Serviço de geolocalização e envio de SMS/Push
        _alertaEnviadoComSucesso.setValue(true);
    }
}